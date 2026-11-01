import com.mytrain.preview.TrainSearch;
public class TrainSearchTest {
 public static void main(String[] args) {
  check(TrainSearch.matches("00001","Sample Express","00001"),"number");
  check(!TrainSearch.matches("00001","Sample Express","1"),"leading zeros");
  check(TrainSearch.matches("00001","Sample Express","  eXpReSs  "),"partial name");
  check(!TrainSearch.matches("00001","Sample Express","absent"),"missing");
  check(!TrainSearch.matches("00001","Sample Express"," "),"empty");
  check(TrainSearch.time(1500).equals("01:00 · Day 2"),"overnight");
  System.out.println("PASS: 6 native-preview search/time assertions");
 }
 static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);}
}
