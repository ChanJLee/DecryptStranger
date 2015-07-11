package com.theOldMen.net;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.theOldMen.zone.TheOldMenUserZoneActivity;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;

/**
 * Created by 李嘉诚 on 2015/4/25.
 * 最后修改时间: 2015/4/25
 */
public class TheOldMenXmlParser {
    ///////////////////////////////////////////////////////////////////////////
    private final static String s_rootElem      = "the_old_men";
    private final static String s_idElem        = "id_content";
    private final static String s_textEle       = "text_content";
    private final static String s_photoEle      = "photo_content";
    private final static String s_nameEle       = "user_name_content";
    private final static String s_encoding      = "utf-8";
    private static BASE64Encoder s_encoder      = new BASE64Encoder();
    private static BASE64Decoder s_decoder      = new BASE64Decoder();
    private static TheOldMenXmlParser s_parser  = null;
    ///////////////////////////////////////////////////////////////////////////

    private TheOldMenXmlParser() {}
    public static TheOldMenXmlParser newInstance() {

        if(s_parser != null)
            return s_parser;

        s_parser = new TheOldMenXmlParser();
        return s_parser;
    }

    ////////////////////////////////////////////////////////////////////////////

    //讲配置 转化为 xml 文件
    public static String configToXML( TheOldMenUserZoneActivity.DataHolder holder) {

        Document doc = new Document();

        //创建根元素 并且设置它
        Element root = new Element(s_rootElem);
        doc.setRootElement(root);

        Element item = new Element(s_textEle);

        try {
            item.addContent(URLEncoder.encode(holder.m_text, s_encoding));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        root.addContent(item);

        item = new Element(s_nameEle);
        try {
            item.addContent(URLEncoder.encode(holder.m_name,s_encoding));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        root.addContent(item);

        item = new Element(s_idElem);
        item.addContent(holder.m_id);
        root.addContent(item);

        item = new Element(s_photoEle);
        item.addContent(bitmapToString(holder.m_image));
        root.addContent(item);

        return new XMLOutputter().outputString(doc);
    }

    //bitmap 到 字符串的转化
    private static String bitmapToString(Bitmap bitmap) {

        //将二进制数据写到比特数组中
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);

        //获得比特数组
        byte[] bytes = stream.toByteArray();

        //转化为字符串
        return s_encoder.encodeBuffer(bytes).trim();
    }

    //字符串到bitmap 的转化
    private static Bitmap stringToBitmap(String str) throws IOException {

        //获得字节数组
        byte[] bytes  = s_decoder.decodeBuffer(str);

        //生成bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        return bitmap;
    }

    //从xml中对组件进行配置
    public static void xmlToConfig(String xml, List<TheOldMenUserZoneActivity.DataHolder> list)
            throws JDOMException, IOException {

        list.clear();

        //创建一个新的字符串
        StringReader xmlReader = new StringReader(xml);
        //创建新的输入源SAX 解析器将使用 InputSource 对象来确定如何读取XML输入
        InputSource xmlSource  = new InputSource(xmlReader);
        //创建一个SAXBuilder
        SAXBuilder builder     = new SAXBuilder();

        //通过输入源SAX构造一个Document
        org.jdom2.Document doc = builder.build(xmlSource);

        //获得根节点
        org.jdom2.Element root = doc.getRootElement();

        //获得body节点下面的所有子节点
        List<Element> children    = root.getChildren();

        //遍历出body节点下面所有的子节点
        for (org.jdom2.Element child : children) {

            TheOldMenUserZoneActivity.DataHolder holder
                    = new TheOldMenUserZoneActivity.DataHolder();

            holder.m_id         = child.getChild(s_idElem).getValue();
            holder.m_text       = child.getChild(s_textEle).getValue();
            holder.m_name       =  URLDecoder.decode(child.getChild(s_nameEle).getValue(), s_encoding);
            holder.m_image      = stringToBitmap(child.getChild(s_photoEle).getValue());

            if(holder.m_text == null) holder.m_text = "";
            else holder.m_text = URLDecoder.decode(holder.m_text,s_encoding);

            list.add(holder);
        }
    }
}
