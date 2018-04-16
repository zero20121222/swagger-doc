package io.terminus.jiddn.swagger;

import com.google.common.base.Throwables;
import io.github.robwin.markup.builder.MarkupLanguage;
import io.github.robwin.swagger2markup.GroupBy;
import io.github.robwin.swagger2markup.Swagger2MarkupConverter;
import org.apache.commons.cli.*;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.SafeMode;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static org.asciidoctor.Asciidoctor.Factory.create;
import static org.asciidoctor.AttributesBuilder.attributes;
import static org.asciidoctor.OptionsBuilder.options;

/**
 * Author: Alan
 * Email: v@terminus.io
 * Description: swagger api文档生成html接口文档
 * Date: Created in 2018/04/16
 */
@SpringBootApplication
public class SwaggerApiHtmlDoc {
    private static final String SWAGGER_API = "http://127.0.0.1:8091/v2/api-docs";

    private static final String SWAGGER_JSON = "./docs/swagger/swagger.json";
    private static final String ASCII_DOCTOR_FILES = "./docs/ascii";

    public static void main(String[] args) throws ParseException, FileNotFoundException {
        Options options = new Options();
        options.addOption("s", true, "swagger api url");
        options.addOption("o", true, "api document output path");
        options.addOption("h", false, "help");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options,args);

        String swaggerApi = SWAGGER_API;
        if (cmd.hasOption("s")){
            swaggerApi =cmd.getOptionValue("s");
        }

        String output = ASCII_DOCTOR_FILES;
        if (cmd.hasOption("o")){
            output =cmd.getOptionValue("o");
        }

        if (cmd.hasOption("h")){
            System.out.println("帮助：\n[-s] swagger api url\n[-o] api document output path\n[-h] help");
            System.exit(0);
        }

        SwaggerApiHtmlDoc.swaggerToView(swaggerApi , output);
    }

    public static void httpRequest() {
        HttpURLConnection httpUrlConn = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            URL url = new URL(SWAGGER_API);
            httpUrlConn = (HttpURLConnection) url.openConnection();
            httpUrlConn.setDoOutput(false);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setUseCaches(false);
            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.connect();

            // 将返回的输入流转换成字符串
            inputStream = httpUrlConn.getInputStream();

            outputStream = new FileOutputStream(SWAGGER_JSON);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            System.out.println(Throwables.getStackTraceAsString(e));
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }

                if(inputStream != null){
                    inputStream.close();
                }

                if(httpUrlConn != null){
                    httpUrlConn.disconnect();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void swaggerToView(String swaggerApi, String output) {
        StringBuffer buffer = new StringBuffer();
        HttpURLConnection httpUrlConn = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(swaggerApi);
            httpUrlConn = (HttpURLConnection) url.openConnection();
            httpUrlConn.setDoOutput(false);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setUseCaches(false);
            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.connect();

            // 将返回的输入流转换成字符串
            inputStream = httpUrlConn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            inputStreamReader.close();

            // 释放资源
            inputStream.close();
            httpUrlConn.disconnect();

            Swagger2MarkupConverter.fromString(buffer.toString())
                    .withPathsGroupedBy(GroupBy.TAGS)// 按tag排序
                    .withMarkupLanguage(MarkupLanguage.ASCIIDOC)// 格式
                    .build()
                    .intoFolder(ASCII_DOCTOR_FILES);// 输出

            outputHtml(output);
        } catch (Exception e) {
            System.out.println(Throwables.getStackTraceAsString(e));
        } finally {
            try {
                if(inputStream != null){
                    inputStream.close();
                }

                if(httpUrlConn != null){
                    httpUrlConn.disconnect();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 导出api的html页面
     */
    private static void outputHtml(String output){
        Asciidoctor asciidoctor = create();
        Attributes attributes = attributes().tableOfContents(true).attribute("toc" , "left").sectionNumbers(true).sourceHighlighter("coderay").get();

        Map<String, Object> options = options().safe(SafeMode.UNSAFE).attributes(attributes).toFile(new File(output + "/swagger.html"))
                .asMap();

        asciidoctor.convert("include::./docs/ascii/overview.adoc[]\n" +
                "include::./docs/ascii/definitions.adoc[]\n" +
                "include::./docs/ascii/paths.adoc[]\n", options);
    }
}
