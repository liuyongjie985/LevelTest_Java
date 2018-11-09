import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Labeling {

    public ArrayList<ArrayList<String>> match_key(int num, String re, String str) {

        ArrayList<ArrayList<String>> temp = new ArrayList();

        Pattern p = Pattern.compile(re);
        Matcher m = p.matcher(str);

        for (int i = 0; i < num; i++) {
            temp.add(new ArrayList());
        }

        while (m.find()) {
            int k = 1;
            for (ArrayList<String> cn_key : temp) {
                cn_key.add(m.group("item" + (k - 1)));
                k++;
            }
        }

        return temp;

    }

}
