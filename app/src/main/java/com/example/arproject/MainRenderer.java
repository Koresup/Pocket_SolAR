package com.example.arproject;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.google.ar.core.Session;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements GLSurfaceView.Renderer {

    CameraPreView mCamera;

    boolean mViewportChanged;
    int mViewportWidth, mViewportHeight;
    RenderCallback mRenderCallback;

    ObjRenderer pika;

    ObjRenderer sun, blackhole;

    Planet mercury, venus, earth, moon, mars, jupiter, saturn, neptune, uranus;

    ObjRenderer mflag, mflag2;
    // 깃발
    List<ObjRenderer> flags = new ArrayList<>();
    List<ObjRenderer> flags2 = new ArrayList<>();

    boolean isImgFind = false;
    boolean planetDraw = false;
    boolean moonDraw = false;
    boolean firstFlag = false;
    boolean secondFlag = false;
    boolean blackholeDraw = false;
    boolean cubeDraw = false;
    boolean sunDraw = false;

    String [] info_mercury = { "수성", "4879.4 km", "7.5 × 10^7 km²", "3.023 × 10^23 ㎏", "0.0352°", "87.9691일", "58.646일", "섭씨 427도", "섭씨 -193도", " 10^−14Mpa", "없음"};
    String [] info_venus = new String[]{ "금성", "12,103.7 km", "4.8 × 10^7 km²", "4.8685×10^24 kg", "177.3°", "224.7일", "243.0158일", "500°C", "467°C", "9.3 Mpa", "없음"};
    String [] info_earth = new String[]{ "지구", "12 756.25 km", "5.1 × 10^8 km²", "5.9722 x 10^24 kg", "23.439 281 1°", "365.256 41일", "23시간 56분 4.1초", "섭씨 57도", "섭씨 -89도", "101.325 kPa", "달" };
    String [] info_moon = new String[]{ "달", "3,476.2 km", "3,793만 km²", "7.342 x 10^22kg", "1.5424°", "27.32166155일", "조석 고정", "섭씨 116.85도", "섭씨 -173.15도", "10^−7 Pa", "없음" };
    String [] info_mars = new String[]{ "화성", "6779 km", "1.4437×10^8 km²", "6.4174×10^23 kg", "25.19°", "686.971일", "24시간 37분 22초", "섭씨 35도", "섭씨 -176도", "0.6~1.0kPa", "포보스, 데이모스"};
    String [] info_jupiter = new String[]{ "목성", "142 984km", "6.1419 × 10^10 km²", "1.899 × 10^27 kg", "3.13°", "4332.59일", "약 9시간 55분", "섭씨 -15.272도", "섭씨 -110도", "20 ~ 200 kPa", "갈릴레이 등 80개"};
    String [] info_saturn = new String[]{ "토성", "120,536km", "4.27×10^10 km²", "5.6846 × 10^26 kg", "26.73°", "29.4571년", "10시간 33분 38초", "섭씨 -113도", "섭씨 -170도", "50 ~ 200kPa", "타이탄 등 83개"};
    String [] info_uranus= new String[]{ "천왕성", "51,118km", "8.084×10^9 km²", "8.6832×10^25 kg", "97.77°", "84.02년", "약 17시간 14분 24초", "섭씨 -216도", "섭씨 -224도", "120 kpa", "티타니아 등 28개"};
    String [] info_neptune = new String[]{ "해왕성", "49,244km", "7.6183×10^9 km2", "1.02413×10^26 kg", " 28.32°", "164.8 년", "16시간 6분 36초", "섭씨 -200도", "섭씨 -218도", "100 MPa", "트리톤 등 14개"};


    ArrayList<Planet> planetList = new ArrayList<>();
    ArrayList<Line> mPaths = new ArrayList<>();

    MainRenderer(Context context, RenderCallback callback){
        mRenderCallback = callback;
        mCamera = new CameraPreView();

        pika = new ObjRenderer(context, "pika.obj", "pika.png");
        // 행성
        sun = new ObjRenderer(context, "sun.obj", "sun.png");
        blackhole = new ObjRenderer(context, "blackhole.obj", "blackhole.png");
        mercury = new Planet(context, "mercury.obj", "mercury.png", "mercury",5790,0.4f, -20f,2f,1/4f, info_mercury);
        venus = new Planet(context, "venus.obj", "venus.png", "venus", 10821, 0.6f, 120f,1.5f,-1/10f, info_venus);
        earth = new Planet(context, "earth.obj", "earth.png","earth",14960, 0.9f, -90f,1f,1f, info_earth);
        moon = new Planet(context, "moon.obj", "moon.png","moon",14962,0.1f,0,1f,1f, info_moon);
        mars = new Planet(context, "mars.obj", "mars.png","mars",22739,1.2f,-35f,1/1.5f,2.3f, info_mars);
        jupiter = new Planet(context, "jupiter.obj", "jupiter.png","jupiter",77792,1.5f,-63f,1/2f,2.5f, info_jupiter);
        saturn = new Planet(context, "saturn.obj", "saturn.png","saturn",143018,2f,20f,1/2.5f,2.4f, info_saturn);
        uranus = new Planet(context, "uranus.obj", "uranus.png","uranus",286933,2.3f,-140f,1/3f,-1.3f, info_uranus);
        neptune = new Planet(context, "neptune.obj", "neptune.png","neptune",450745,2.6f,-43f,1/3.5f,1.2f, info_neptune);

        planetList.add(mercury);
        planetList.add(venus);
        planetList.add(earth);
        planetList.add(mars);
        planetList.add(jupiter);
        planetList.add(saturn);
        planetList.add(uranus);
        planetList.add(neptune);

        //깃발
        flags.add(new ObjRenderer(context, "penguin.obj", "Penguin.png"));
        flags2.add(new ObjRenderer(context, "penguin.obj", "Penguin.png"));
        flags.add(new ObjRenderer(context, "andy.obj", "andy.png"));
        flags2.add(new ObjRenderer(context, "andy.obj", "andy.png"));

        mflag = flags.get(0);
        mflag2 = flags2.get(0);

    }

    interface RenderCallback{
        void preRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(1.0f,1.0f, 0.0f, 0.0f);

        mCamera.init();
        pika.init();
        sun.init();
        moon.init();
        blackhole.init();
        for(ObjRenderer obj : planetList){
            obj.init();
        }
        for(int i=0; i<flags.size(); i++){
           flags.get(i).init();
           flags2.get(i).init();

        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        mViewportChanged = true;
        mViewportWidth = width;
        mViewportHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);

        mRenderCallback.preRender();

        GLES20.glDepthMask(false);
        mCamera.draw();

        GLES20.glDepthMask(true);
        if(isImgFind){
            if(sunDraw) {
                sun.draw();
            }


            if(blackholeDraw){
                blackhole.draw();
            }

            if(planetDraw){
                for(ObjRenderer obj : planetList){
                    obj.draw();
                }

                if(moonDraw) {
                    moon.draw();
                }
            }

            if(firstFlag){
                mflag.draw();
            }
            if(secondFlag) {
                mflag2.draw();
            }
        }


        pika.draw();
    }




    void updateSession(Session session, int displayRotation){
        if (mViewportChanged) {
            session.setDisplayGeometry(displayRotation, mViewportWidth, mViewportHeight);
            mViewportChanged = false;
        }
    }

    void setProjectionMatrix(float[] matrix){
        sun.setProjectionMatrix(matrix);
        blackhole.setProjectionMatrix(matrix);
        moon.setProjectionMatrix(matrix);
        pika.setProjectionMatrix(matrix);


        for(ObjRenderer obj : planetList){
            obj.setProjectionMatrix(matrix);
        }
        for(int i=0; i<flags.size(); i++){
            flags.get(i).setProjectionMatrix(matrix);
            flags2.get(i).setProjectionMatrix(matrix);
        }
    }

    void updateViewMatrix(float[] matrix){
        sun.setViewMatrix(matrix);
        blackhole.setViewMatrix(matrix);
        moon.setViewMatrix(matrix);
        pika.setViewMatrix(matrix);

        for(ObjRenderer obj : planetList){
            obj.setViewMatrix(matrix);
        }
        for(int i=0; i<flags.size(); i++){
            flags.get(i).setViewMatrix(matrix);
            flags2.get(i).setViewMatrix(matrix);
        }

        for (Line line : mPaths) {
            line.updateViewMatrix(matrix);
        }
    }

    int getTextureId(){
        return mCamera == null ? -1 : mCamera.mTextures[0];
    }

    void objChanged(int index, float[] firstMatrix, float[] secondMatrix) {
        if (secondMatrix != null) {
            mflag = flags.get(index);
            mflag.setModelMatrix(firstMatrix);
            mflag2 = flags2.get(index);
            mflag2.setModelMatrix(secondMatrix);
        } else {
            mflag = flags.get(index);
            mflag.setModelMatrix(firstMatrix);
            mflag2 = flags2.get(index);
        }
    }
}
