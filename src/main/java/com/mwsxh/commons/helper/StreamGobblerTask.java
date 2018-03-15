package com.mwsxh.commons.helper;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class StreamGobblerTask implements Callable<List<String>> {

    InputStream _is;
    String _type;
    OutputStream _os;

    public StreamGobblerTask(InputStream is, String type) {
        this(is, type, null);
    }

    StreamGobblerTask(InputStream is, String type, OutputStream redirect) {
        _is = is;
        _type = type;
        _os = redirect;
    }

    /**
     * 获取命令行的输出结果。
     *
     * @return 命令行输出结果
     */
    public List<String> call() {
        List<String> lines = new ArrayList<>();
        try {
            PrintWriter writer = null;
            if (_os != null) {
                writer = new PrintWriter(_os);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(_is));

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (writer != null) {
                    writer.println(line);
                }
                System.out.println(_type + "> " + line);
                lines.add(line);
            }

            if (writer != null) {
                writer.flush();
            }
        } catch (IOException e) {
            log.error("", e);
        }
        return lines;
    }

}
