import java.io.File;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaTest {

    public static void main(String[] args) {

        StringBuffer sb = new StringBuffer();
        Pattern p = Pattern.compile("\\?P<.+?>");
        Matcher m = p.matcher("(?P<ldd_flow_used>[MKGB]*)(?P<ldd_flow_left_2>[KMGB]*)");
        int i = 0;
        while (m.find()) {
            String tmp = m.group();
            String v = "?<item" + i + ">";
            //注意，在替换字符串中使用反斜线 (\) 和美元符号 ($) 可能导致与作为字面值替换字符串时所产生的结果不同。
            //美元符号可视为到如上所述已捕获子序列的引用，反斜线可用于转义替换字符串中的字面值字符。
            //替换掉查找到的字符串
            m.appendReplacement(sb, v);
            i += 1;
        }
        //别忘了加上最后一点
        m.appendTail(sb);
        System.out.print(sb);
    }
}