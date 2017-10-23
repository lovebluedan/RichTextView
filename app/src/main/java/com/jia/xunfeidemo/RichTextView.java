package com.jia.xunfeidemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Description: 富文本展示  讯飞语音阅读
 * Created by jia on 2017/10/20.
 * 人之所以能，是相信能
 */
public class RichTextView extends EditText {

    private static final String TAG = "RichTextView";

    private HashMap<String, Drawable> imgs = new HashMap<>();

    private NetWorkImageGetter mNetWorkImageGetter = new NetWorkImageGetter();

    private Handler handler = new Handler();

    private int img_num = 0;

    private int[] span;

    private String[] lines;

    private String text;

    //语音合成对象
    private SpeechSynthesizer mSpeechSynthesizer;

    // 默认云端发音人
    public static String voicerCloud = "xiaoyan";
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    public RichTextView(Context context) {
        super(context);
        init();
    }

    public RichTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RichTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        mSpeechSynthesizer = SpeechSynthesizer.createSynthesizer(getContext(), new InitListener() {
            @Override
            public void onInit(int i) {
                Log.e(TAG, "onInit: " + i);
            }
        });
    }

    public void fromHtml(String text){

        this.text=text;

        setText(Html.fromHtml(text, mNetWorkImageGetter, new Html.TagHandler() {
            @Override
            public void handleTag(boolean b, String s, Editable editable, XMLReader xmlReader) {
                Log.e(TAG, "handleTag: "+s );
                if (s.equals("img")) {
                    img_num++;
                }
            }
        }));

        // 没有图片直接加载
        if(img_num==0){
            setText();
        }
    }


    class NetWorkImageGetter implements Html.ImageGetter {

        @Override
        public Drawable getDrawable(final String source) {

            Log.e(TAG, "getDrawable: " );

            if (imgs.containsKey(source)) {
                imgs.get(source).setBounds(0, 0, imgs.get(source).getIntrinsicWidth() * 2,
                        imgs.get(source).getIntrinsicHeight() * 2);
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        imgs.put(source, new BitmapDrawable(getbitmap(source)));

                        if (imgs.size() == img_num) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setText();
                                }
                            });
                        }
                    }
                }).start();
            }

            return imgs.get(source);
        }

    }

    private void setText(){
        Log.e(TAG, "setText: " );
        lines = getText().toString().split("。|？|！|@|···|;|；|!");

        if (lines != null && lines.length > 0) {

            span = new int[lines.length];
            for (int i = 0; i < lines.length; i++) {
                Log.e(TAG, "run: "+i+" "+lines[i] );
                if (i == 0) {
                    span[i] = 0;
                } else {
                    span[i] = span[i - 1] + lines[i - 1].length() + 1;
                }

            }

        }

        setText(Html.fromHtml(text, mNetWorkImageGetter, null));

        SpannableStringBuilder style = new SpannableStringBuilder(getText());
        for (int i = 0; i < span.length; i++) {
            if(i==span.length-1){
                style.setSpan(new TextViewURLSpan(i), span[i], getText().length()-1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else{
                style.setSpan(new TextViewURLSpan(i), span[i], span[i+1]-1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

        }
        setText(style);
        setMovementMethod(LinkMovementMethod.getInstance());
    }

    private class TextViewURLSpan extends ClickableSpan {
        int flag;

        public TextViewURLSpan(int flag) {
            this.flag = flag;
        }

        @Override
        public void updateDrawState(TextPaint ds) {

            ds.setUnderlineText(false); //去掉下划线
        }

        @Override
        public void onClick(View widget) {//点击事件
            Log.e(TAG, "onClick: "+flag);

            // 语音合成
            mSpeechSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
            mSpeechSynthesizer.setParameter(SpeechConstant.ENGINE_MODE, mEngineType);

            mSpeechSynthesizer.setParameter(SpeechConstant.VOICE_NAME, voicerCloud);

            mSpeechSynthesizer.startSpeaking(lines[flag], new SynthesizerListener() {
                @Override
                public void onSpeakBegin() {

                }

                @Override
                public void onBufferProgress(int i, int i1, int i2, String s) {

                }

                @Override
                public void onSpeakPaused() {

                }

                @Override
                public void onSpeakResumed() {

                }

                @Override
                public void onSpeakProgress(int i, int i1, int i2) {

                }

                @Override
                public void onCompleted(SpeechError speechError) {
                }

                @Override
                public void onEvent(int i, int i1, int i2, Bundle bundle) {

                }
            });
        }
    }

    /**
     * 根据一个网络连接(String)获取bitmap图像
     *
     * @param imageUri
     * @return
     */
    public static Bitmap getbitmap(String imageUri) {

        // 显示网络上的图片
        Bitmap bitmap = null;
        try {
            URL myFileUrl = new URL(imageUri);
            HttpURLConnection conn = (HttpURLConnection) myFileUrl
                    .openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();

        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            bitmap = null;
        } catch (IOException e) {
            e.printStackTrace();
            bitmap = null;
        }
        return bitmap;
    }

    @Override
    protected boolean getDefaultEditable() {//禁止EditText被编辑
        return false;
    }
}
