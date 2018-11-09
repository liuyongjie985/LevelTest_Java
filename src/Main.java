import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;


import java.io.FileNotFoundException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.google.gson.JsonArray;

import com.google.gson.JsonIOException;

import com.google.gson.JsonObject;

import com.google.gson.JsonParser;

import com.google.gson.JsonSyntaxException;


/*

输入文件：data/0827_phone_flow1.xlsx
输入文件格式：
A                        B                  C                   D                   E
端口号(无关内容)      模板主题(无关内容)      短信原文           模板内容(无关内容)        省份(无关内容)

配置文件：'data/kre.json'，'data/kre_list2.json' 在同级目录中

输出文件:
levelResult.txt

*/











public class Main {


    public static void main(String[] args) {
        Workbook wb = null;
        Sheet sheet = null;
        Row row = null;
        List<Map<String, String>> list = null;
        String cellData = null;
        String filePath = "./data/0827话费流量类数据1.xlsx";
        String columns[] = {"归属端口号", "模板主题", "短信原文", "模板内容", "省份"};
        wb = read_excel(filePath);
        if (wb != null) {
            //用来存放表中数据
            list = new ArrayList<Map<String, String>>();
            //获取第一个sheet
            sheet = wb.getSheetAt(0);
            //获取最大行数
            int rownum = sheet.getPhysicalNumberOfRows();
            //获取第一行
            row = sheet.getRow(0);
            //获取最大列数
            int colnum = row.getPhysicalNumberOfCells();
            for (int i = 1; i < rownum; i++) {
                Map<String, String> map = new LinkedHashMap<String, String>();
                row = sheet.getRow(i);
                if (row != null) {
                    for (int j = 0; j < colnum; j++) {
                        cellData = (String) get_cell_format_value(row.getCell(j));
                        map.put(columns[j], cellData);
                    }
                } else {
                    break;
                }
                list.add(map);
            }
        }


        ArrayList<Map> records = JavaToJson("./data/kre.json", 0);
        ArrayList<Map> sp_records = JavaToJson("./data/kre_list2.json", 1);

        ArrayList<String> re_list = new ArrayList<>();
        ArrayList<Integer> add_re_list = new ArrayList<>();
//        将records转换成java能用的形式
        for (Map<String, String> regex : records) {

            String temp = regex.get("re");

            StringBuffer sb = new StringBuffer();
            Pattern p = Pattern.compile("\\?P<.+?>");
            Matcher m = p.matcher(temp);
            int i = 0;
            while (m.find()) {
                m.group();
                String v = "?<item" + i + ">";
                //注意，在替换字符串中使用反斜线 (\) 和美元符号 ($) 可能导致与作为字面值替换字符串时所产生的结果不同。
                //美元符号可视为到如上所述已捕获子序列的引用，反斜线可用于转义替换字符串中的字面值字符。
                //替换掉查找到的字符串
                m.appendReplacement(sb, v);
                i += 1;
            }
            //别忘了加上最后一点
            m.appendTail(sb);
            System.out.println(sb);
            re_list.add(sb.toString());
            add_re_list.add(i);


        }

        ArrayList<String> sp_re_list = new ArrayList<>();
        ArrayList<Integer> add_sp_re_list = new ArrayList<>();
//将sp_records转换成java能用的表达式
        for (Map<String, String> regex : sp_records) {

            String temp = regex.get("re");
            StringBuffer sb = new StringBuffer();
            Pattern p = Pattern.compile("\\?P<.+?>");
            Matcher m = p.matcher(temp);
            int i = 0;
            while (m.find()) {
                m.group();
                String v = "?<item" + i + ">";

                m.appendReplacement(sb, v);
                i += 1;
            }
            //别忘了加上最后一点
            m.appendTail(sb);
            System.out.println(sb);

            sp_re_list.add(sb.toString());
            add_sp_re_list.add(i);
        }


        File file = new File("levelResult.txt");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            Labeling l = new Labeling();

            //遍历解析出来的list
            for (Map<String, String> map : list) {

                String line = map.get("短信原文");

                //                System.out.print(entry.getKey() + ":" + entry.getValue() + ",");
                Map<String, ArrayList<String>> key_value = new HashMap<>();
                int num = 0;
                int idx = 0;
                for (Map<String, String> regex : records) {
                    String temp_key = regex.get("key");
                    if (temp_key.substring(0, 6).equalsIgnoreCase("jsheng")) {

                        ArrayList<Integer> my_list = searchAllIndex(temp_key, "_");


                        if (my_list.size() == 2) {
                            temp_key = temp_key.substring(my_list.get(0) + 1, my_list.get(1));
                        }
                    }


                    ArrayList<ArrayList<String>> result = l.match_key(add_re_list.get(idx), re_list.get(idx), line);


                    if (result.get(0).size() != 0) {
                        if (!key_value.containsKey(regex.get("cn_key"))) {
                            num += 1;
                            key_value.put(regex.get("cn_key"), result.get(0));
                        }


                    }
                    idx++;

                }
                idx = 0;
                for (Map<String, String> regex : sp_records) {
                    String[] temp_key_list = regex.get("cn_key").split(" ");
                    ArrayList<String> key_list = new ArrayList<>();
                    for (int i = 0; i <= temp_key_list.length; i++) {
                        key_list.add("item" + (i + 1));

                    }


                    ArrayList<ArrayList<String>> result = l.match_key(add_sp_re_list.get(idx), sp_re_list.get(idx), line);


                    if (result.get(0).size() != 0) {
                        for (int k = 0; k < temp_key_list.length; k++) {

                            num += 1;
                            key_value.put(temp_key_list[k], result.get(k));

                        }

                    }
                    idx++;
                }


                int total = calculateSegment(line);

                double level = num / (total + 0.00001);
                if (level > 1) {
                    level = 1;
                }


                fos.write(line.getBytes());
                fos.write("\t\t".getBytes());
                fos.write(String.valueOf(level).getBytes());
                fos.write("\n".getBytes());
                for (Map.Entry<String, ArrayList<String>> entry : key_value.entrySet()) {
                    fos.write(entry.getKey().getBytes());
                    fos.write(":".getBytes());
                    for (String x : entry.getValue()) {
                        fos.write(x.getBytes());
                        fos.write("\t".getBytes());

                    }
                    fos.write("\n".getBytes());

                }
                fos.write("###############################################################################\n".getBytes());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    static private int calculateSegment(String line) {
        int num = 0;
        String[] result = line.split("，|。|！|：|；");

        Pattern p = Pattern.compile("(\\d+)");

        for (String x : result) {
            Matcher m = p.matcher(x);
            if (m.find()) {

                num += 1;
            }

        }


        return num;
    }

    //读取excel
    public static Workbook read_excel(String filePath) {
        Workbook wb = null;
        if (filePath == null) {
            return null;
        }
        String extString = filePath.substring(filePath.lastIndexOf("."));
        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
            if (".xls".equals(extString)) {
                return wb = new HSSFWorkbook(is);
            } else if (".xlsx".equals(extString)) {
                return wb = new XSSFWorkbook(is);
            } else {
                return wb = null;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wb;
    }

    public static Object get_cell_format_value(Cell cell) {
        Object cellValue = null;
        if (cell != null) {
            //判断cell类型
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_NUMERIC: {
                    cellValue = String.valueOf(cell.getNumericCellValue());
                    break;
                }
                case Cell.CELL_TYPE_FORMULA: {
                    //判断cell是否为日期格式
                    if (DateUtil.isCellDateFormatted(cell)) {
                        //转换为日期格式YYYY-mm-dd
                        cellValue = cell.getDateCellValue();
                    } else {
                        //数字
                        cellValue = String.valueOf(cell.getNumericCellValue());
                    }
                    break;
                }
                case Cell.CELL_TYPE_STRING: {
                    cellValue = cell.getRichStringCellValue().getString();
                    break;
                }
                default:
                    cellValue = "";
            }
        } else {
            cellValue = "";
        }
        return cellValue;
    }


    static private ArrayList<Integer> searchAllIndex(String str, String key) {
        ArrayList<Integer> result = new ArrayList<>();

        int a = str.indexOf(key);//*第一个出现的索引位置
        while (a != -1) {
            result.add(a);
            a = str.indexOf(key, a + 1);//*从这个索引往后开始第一个出现的位置
        }


        return result;
    }


    public static ArrayList<Map> JavaToJson(String fileName, int sign) {


//将 test.json 的数据转换成 JSON 对象

//需要创建一个解析器，可以用来解析字符串或输入流

        JsonParser parser = new JsonParser();
        ArrayList<Map> result = new ArrayList<>();

        try {


//创建一个JSON对象，接收parser解析后的返回值

//使用parse()方法，传入一个Reader对象，返回值是JsonElement类型

//因为要读取文件，所以传入一个FileReader

//JsonObject是JsonElement的子类，所以需要强转

//有异常抛出，使用 try catch 捕获

            JsonObject object = (JsonObject) parser.parse(new FileReader(fileName));


//先将两个外部的属性输出 category 和 pop

//先 get 到名称（键），返回的是 JsonElement，再 getAs 转换成什么类型的值

//依据 json 格式里的数据类型

//            System.out.println("category=" + object.get("category").getAsString());

//            System.out.println("pop=" + object.get("pop").getAsBoolean());


//接着读取test.json里的JSON数组，名称是languages（键）

//创建一个JsonArray

            JsonArray array = object.get("RECORDS").getAsJsonArray();

            for (int i = 0; i < array.size(); i++) {

//分隔线
                Map<String, String> temp = new HashMap<>();

//                System.out.println("-----------------");

//创建一个JsonObject，从array的下标获取，get() 返回JsonElement类型

//这里不用强转，而用 getAsJsonObject() 进行转换

                JsonObject subObject = array.get(i).getAsJsonObject();


//                {
//                    "cn_key": "业务说明",
//                        "key": "jsheng_业务说明_1",
//                        "re": "业务说明[:：](?P<业务说明>(.*))",
//                        "level": "0000"
//                },
                if (sign == 0) {
                    temp.put("cn_key", subObject.get("cn_key").getAsString());
                    temp.put("key", subObject.get("key").getAsString());
                    temp.put("re", subObject.get("re").getAsString());
                    temp.put("level", subObject.get("level").getAsString());

                } else {
                    temp.put("key", subObject.get("key").getAsString());
                    temp.put("sence", subObject.get("sence").getAsString());
                    temp.put("cn_key", subObject.get("cn_key").getAsString());
                    temp.put("re", subObject.get("re").getAsString());
                }
                result.add(temp);

            }


        } catch (JsonIOException e) {

            e.printStackTrace();

        } catch (JsonSyntaxException e) {

            e.printStackTrace();

        } catch (FileNotFoundException e) {

            e.printStackTrace();

        }
        return result;
    }
}













