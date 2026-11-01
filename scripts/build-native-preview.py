"""Build a standalone standard-Android demo with the already verified local SDK.
No Gradle, React Native AAR, hosted build, or network is needed.
"""
import os, pathlib, shutil, subprocess, tempfile, zipfile, hashlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
APP=ROOT/'apps/native-preview'
SDK=pathlib.Path('/workspace/toolchains/android-sdk')
TOOLS=SDK/'build-tools/37.0.0'
JDK=pathlib.Path('/workspace/toolchains/java/jdk-21.0.13+9')
ANDROID=SDK/'platforms/android-37.0/android.jar'
OUT=ROOT/'artifacts'
ENV={**os.environ,'JAVA_HOME':str(JDK),'PATH':str(JDK/'bin')+os.pathsep+os.environ['PATH']}
def run(*args):subprocess.run([str(x) for x in args],check=True,env=ENV)
OUT.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='mytrain-native-apk-') as temporary:
 t=pathlib.Path(temporary);(t/'assets').mkdir();(t/'classes').mkdir();(t/'dex').mkdir()
 shutil.copy2(ROOT/'data/demo.json',t/'assets/demo.json')
 run(TOOLS/'aapt2','compile','--dir',APP/'res','-o',t/'resources.zip')
 run(TOOLS/'aapt2','link','-o',t/'unsigned.apk','--manifest',APP/'AndroidManifest.xml','-I',ANDROID,'-A',t/'assets',t/'resources.zip')
 run(JDK/'bin/javac','-source','8','-target','8','-classpath',ANDROID,'-d',t/'classes',*sorted((APP/'src').rglob('*.java')))
 run(JDK/'bin/jar','--create','--file',t/'classes.jar','-C',t/'classes','.')
 run(TOOLS/'d8','--lib',ANDROID,'--min-api','24','--output',t/'dex',t/'classes.jar')
 with zipfile.ZipFile(t/'unsigned.apk','a',compression=zipfile.ZIP_DEFLATED) as apk:apk.write(t/'dex/classes.dex','classes.dex')
 run(TOOLS/'zipalign','-f','4',t/'unsigned.apk',t/'aligned.apk')
 signing=ROOT/'.preview-signing';signing.mkdir(exist_ok=True)
 key=signing/'debug.keystore'
 if not key.exists():run(JDK/'bin/keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','365','-dname','CN=Mytrain Internal Demo','-storetype','JKS')
 output=OUT/'mytrain-native-preview.apk'
 run(TOOLS/'apksigner','sign','--ks',key,'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',output,t/'aligned.apk')
 run(TOOLS/'apksigner','verify','--verbose',output)
 run(TOOLS/'zipalign','-c','4',output)
 run(TOOLS/'aapt2','dump','badging',output)
 digest=hashlib.sha256(output.read_bytes()).hexdigest();(OUT/'mytrain-native-preview.apk.sha256').write_text(digest+'  mytrain-native-preview.apk\n')
 print('APK:',output,'SHA256:',digest)
