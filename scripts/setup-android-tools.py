"""Restore the verified Linux x64 local toolchains used by the native demo build.
Downloads only missing toolchains, preserves existing directories, never installs a VPN.
"""
import hashlib
import pathlib
import subprocess
import tarfile
import tempfile
import urllib.request
import zipfile

ROOT = pathlib.Path('/workspace/toolchains')
SDK = ROOT / 'android-sdk'
JDKS = [
    ('17.0.21+9', 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.21_9.tar.gz', '8b9b611d917e3dfd8419d3566b5949226f69b052fed7e109c1368add1dfe8711'),
    ('21.0.13+9', 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz', 'd7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c'),
]

def download(url, expected, target):
    digest = hashlib.sha256()
    with urllib.request.urlopen(url, timeout=60) as source, target.open('wb') as output:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
            output.write(chunk)
    if digest.hexdigest() != expected:
        raise RuntimeError('Official artifact checksum mismatch; refusing installation.')

with tempfile.TemporaryDirectory(prefix='mytrain-tools-') as temporary:
    temp = pathlib.Path(temporary)
    sdk_binary = SDK / 'cmdline-tools/latest/bin/android'
    if not sdk_binary.exists():
        destination = SDK / 'cmdline-tools/latest'
        if destination.exists():
            raise RuntimeError('Existing incomplete SDK tools directory: inspect before replacing.')
        archive = temp / 'tools.zip'
        download('https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip', '0877a1d048fe4a24efe2eff536ca4223f7adeb58648bb81909d33c446918cfa8', archive)
        with zipfile.ZipFile(archive) as package:
            for member in package.infolist():
                if not member.filename.startswith('cmdline-tools/'):
                    continue
                path = destination / member.filename.removeprefix('cmdline-tools/')
                if not path.resolve().is_relative_to(destination.resolve()):
                    raise RuntimeError('Invalid SDK archive path')
                if member.is_dir():
                    path.mkdir(parents=True, exist_ok=True)
                else:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(package.read(member))
                    if '/bin/' in str(path):
                        path.chmod(0o755)
    for version, url, checksum in JDKS:
        destination = ROOT / 'java' / ('jdk-' + version)
        if (destination / 'bin/javac').exists():
            continue
        if destination.exists():
            raise RuntimeError('Existing incomplete JDK: inspect before replacing.')
        archive = temp / ('jdk-' + version + '.tar.gz')
        download(url, checksum, archive)
        (ROOT / 'java').mkdir(parents=True, exist_ok=True)
        with tarfile.open(archive) as package:
            package.extractall(ROOT / 'java', filter='data')
    packages = [
        ('platforms/android-37.0', 'platforms/android-37.0/android.jar'),
        ('build-tools/37.0.0', 'build-tools/37.0.0/apksigner'),
        ('build-tools/36.0.0', 'build-tools/36.0.0/apksigner'),
        ('platform-tools@37.0.1', 'platform-tools/adb'),
        ('ndk/27.1.12297006', 'ndk/27.1.12297006/ndk-build'),
        ('cmake/3.22.1', 'cmake/3.22.1/bin/cmake'),
    ]
    for package, sentinel in packages:
        if not (SDK / sentinel).exists():
            subprocess.run([str(sdk_binary), '--no-metrics', '--sdk=' + str(SDK), 'sdk', 'install', package], check=True)
            if not (SDK / sentinel).exists():
                raise RuntimeError('SDK installer did not create the expected package: ' + package)
print('Local Android toolchain prerequisites are present. No hosted build or store service configured.')
