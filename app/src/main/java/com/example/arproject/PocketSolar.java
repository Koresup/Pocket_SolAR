package com.example.arproject;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class PocketSolar extends AppCompatActivity {
    final String INIT = "init";                         // 초기상태 (이미지 인식 가능)     이미지 인식만 가능 (버튼 보이지 않음)
    final String IMAGE_SCAN = "imageScan";              // 이미지 스캔 (태양 떠오르는 중)  이미지 인식 불가, 나가기, 공전 자전 버튼 보이기
    final String RISING_SUN = "risingSun";              // 태양이 다 떠오른 후 (돌아요)    행성 obj 출력
    final String PLANET_MOVE_OFF = "off";               // 행성 배치 완료                  이미지 인식 불가, 초기화, 공전/자전, 정보조회, 거리조회 가능
    final String PLANET_MOVE_ON = "on";                 // 행성 공전, 자전 상태            이미지 인식 불가, 초기화, 공전/자전, 정보조회, 거리조회 가능
    final String PLANET_INFO = "info";                  // 행성 정보조회                   이미지 인식 불가, 공전/자전 멈춤, 거리조회, 초기화 불가, 기존 obj draw off
    // ui변경, 선택된 행성 obj 생성, 정보창 VISIBLE, 행성 rotateM() 가능, 나가기 버튼 -> 이전 상태(move on/off) 이동
    final String PLANET_DISTANCE_ONE = "distanceOne";   // 행성 간 거리조회 1회 터치       이미지 인식 불가, 공전/자전 멈춤, 정보조회, 초기화 불가, 새로고침(취소)버튼 생성, 깃발 변경 가능
    final String PLANET_DISTANCE_INFO = "distanceInfo"; // 행성 간 거리 조회 중            이미지 인식 불가, 공전/자전 멈춤, 정보조회, 초기화 불가, 새로고침 가능, 새로고침 후 이전 상태(on/off)
    final String PLANET_REMOVE = "remove";              // 행성 삭제 (블랙홀)              이미지 인식 불가, 공전/자전 멈춤, 정보조회, 초기화 불가, 일정시간 후 init 상태 도달

    String prevState = "";
    String state = INIT;

    Button rotateBtn;

    GLSurfaceView mSurfaceView;
    MainRenderer mRenderer;

    Session mSession;
    Config mConfig;

    boolean mUserRequestedInstall = true;
    LinearLayout btnLayout;
    LinearLayout flagBtnLayout;
    TextView text1, text2, comment;

    float[] mePos = new float[3];

    //////////////////////////////////////////////////////////////////////////////////////
    float[] firstPlanetCenter;
    float[] secondPlanetCenter;
    ImageButton pengBtn, andyBtn;

    boolean isModelInit = false, mCatched = false;
    float mCatchX, mCatchY;
    float borderPointY;

    GestureDetector mGestureDetector;

    float[] firstFlagMatrix = new float[16];
    float[] secondFlagMatrix = new float[16];
    float[] lineModelMatrix = new float[16];


    ImageButton flagResetBtn;

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    LinearLayout layout_info;
    ImageButton info_cancel;
    TextView txt_korName, txt_engName, txt_distance, txt_surface, txt_weight, txt_lean,
            txt_revolve, txt_rotate, txt_maxCel, txt_minCel, txt_press, txt_satellite;

    boolean doubleTap = false;
    boolean longTouch = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndTitleBar();
        setContentView(R.layout.pocketsolar);

        mSurfaceView = findViewById(R.id.glsurfaceview);
        rotateBtn = findViewById(R.id.rotateBtn);
        btnLayout = findViewById(R.id.btnLayout);
        pengBtn =  findViewById(R.id.pengBtn);
        andyBtn =  findViewById(R.id.andyBtn);
        flagResetBtn =findViewById(R.id.flagResetBtn);

        flagBtnLayout = findViewById(R.id.flagBtnLayout);

        text1 = findViewById(R.id.state);
        text2 = findViewById(R.id.eventText);
        comment = findViewById(R.id.comment);

        layout_info = (LinearLayout) findViewById(R.id.layout_info);
        info_cancel = (ImageButton) findViewById(R.id.info_cancel);

        txt_korName = (TextView) findViewById(R.id.txt_korName);
        txt_engName = (TextView) findViewById(R.id.txt_engName);
        txt_distance = (TextView) findViewById(R.id.txt_distance);
        txt_surface = (TextView) findViewById(R.id.txt_surface);
        txt_weight = (TextView) findViewById(R.id.txt_weight);
        txt_lean = (TextView) findViewById(R.id.txt_lean);
        txt_revolve = (TextView) findViewById(R.id.txt_revolve);
        txt_rotate = (TextView) findViewById(R.id.txt_rotate);
        txt_maxCel = (TextView) findViewById(R.id.txt_maxCel);
        txt_minCel = (TextView) findViewById(R.id.txt_minCel);
        txt_press = (TextView) findViewById(R.id.txt_press);
        txt_satellite = (TextView) findViewById(R.id.txt_satellite);

        flagBtnLayout.setVisibility(View.INVISIBLE);
        btnLayout.setVisibility(View.INVISIBLE);
//        comment.setVisibility(View.INVISIBLE);

        info_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout_info.setVisibility(View.GONE);
                state = prevState;

                if(state.equals(PLANET_MOVE_ON)){
                    rotateFlag = true;
                    rotateThreadOn();
                }
            }
        });

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent event) {

                Log.d("롱터치","두둥");
                mCatchX = event.getX();
                mCatchY = event.getY();

                mCatched = true;
                longTouch = true;

            }

            // 더블 터치를 통한 오른쪽 화면에 정보 레이아웃 생성
            @Override
            public boolean onDoubleTap(MotionEvent event) {

                mCatchX = event.getX();
                mCatchY = event.getY();
                Log.d("따블 클릭", "오케이!");

                doubleTap = true;
                mCatched = true;

                return true;
            }
        });

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);

        if(displayManager != null){
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {

                }

                @Override
                public void onDisplayRemoved(int displayId) {

                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized(this){
                        mRenderer.mViewportChanged = true;
                    }
                }
            },null);
        }

        mRenderer = new MainRenderer(this, new MainRenderer.RenderCallback() {
            @Override
            public void preRender() {
                if(mRenderer.mViewportChanged){
                    Display display = getWindowManager().getDefaultDisplay();
                    int displayRotation = display.getRotation();
                    mRenderer.updateSession(mSession, displayRotation);
                }

                mSession.setCameraTextureName(mRenderer.getTextureId());

                Frame frame = null;
                try{
                    frame = mSession.update();
                } catch (CameraNotAvailableException e){
                    e.printStackTrace();
                }

                if(frame.hasDisplayGeometryChanged()){
                    mRenderer.mCamera.transformDisplayGeometry(frame);
                }

                Camera camera = frame.getCamera();
                float [] projMatrix = new float[16];
                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f);
                float[] viewMatrix = new float[16];
                camera.getViewMatrix(viewMatrix, 0);

                mRenderer.setProjectionMatrix(projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);

                mRenderer.andy.setViewMatrix(viewMatrix);

                mePos = calculateInitialMePoint(
                        mRenderer.mViewportWidth,
                        mRenderer.mViewportHeight,
                        projMatrix,
                        viewMatrix
                );

                float[] modelMatrix = new float[16];
                // matrix 초기화
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix,0, mePos[0], mePos[1], mePos[2]);
                Matrix.scaleM(modelMatrix,0, 0.2f, 0.2f, 0.2f);
                mRenderer.andy.setModelMatrix(modelMatrix);

                // 이미지추적결과에 따른 그리기 설정
                if(!drawTag) {
                    drawImage(frame);
                }

                if (mCatched) {
                    if(!longTouch&&doubleTap){
                        if(state.equals(PLANET_MOVE_ON) || state.equals(PLANET_MOVE_OFF)||state.equals(PLANET_INFO)) {
                            mCatched = false;
                            List<HitResult> results2 = frame.hitTest(mCatchX, mCatchY);
                            for (HitResult result : results2) {
                                Pose pose = result.getHitPose();  //증강공간에서의 좌표
                                if (catchCheck(pose.tx(),pose.ty(),pose.tz(), "doubleTap")) {
                                    prevState = state;
                                    state = PLANET_INFO;
                                    rotateFlag = false;

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            txt_korName.setText(name_kor);
                                            txt_engName.setText(name_eng);
                                            txt_distance.setText(info_distance);
                                            txt_surface.setText(info_surface);
                                            txt_weight.setText(info_weight);
                                            txt_lean.setText(info_lean);
                                            txt_revolve.setText(info_revolve);
                                            txt_rotate.setText(info_rotate);
                                            txt_maxCel.setText(info_maxCel);
                                            txt_minCel.setText(info_minCel);
                                            txt_press.setText(info_press);
                                            txt_satellite.setText(info_satellite);
                                            layout_info.setVisibility(View.VISIBLE);
                                            // 정보 설정
                                            text2.setText("정보보기");
                                        }
                                    });
                                    break;
                                }

                            }
                            doubleTap = false;
                        }
                    }
                    else if(!doubleTap&&longTouch) {
                        List<HitResult> results = frame.hitTest(mCatchX, mCatchY);
                        if (state.equals(PLANET_MOVE_ON) || state.equals(PLANET_MOVE_OFF)){
                            for (HitResult result : results) {
                                Pose pose = result.getHitPose();
                                if (catchCheck(pose.tx(), pose.ty(), pose.tz(), "longTouch")) {
                                    Matrix.translateM(firstFlagMatrix, 0, 0f, borderPointY, 0f);
                                    mRenderer.mflag.setModelMatrix(firstFlagMatrix);
                                    prevState = state;
                                    state = PLANET_DISTANCE_ONE;
                                    mCatched = false;
                                    mRenderer.flagDraw =true;
                                    rotateFlag = false;
                                    messageFlag = true;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            flagBtnLayout.setVisibility(View.VISIBLE);
                                            text2.setText("1번 플래그 뜸");
                                        }
                                    });
                                    break;
                                }
                            }
                            longTouch = false;
                        } else if (state.equals(PLANET_DISTANCE_ONE)){
                            for (HitResult result : results) {
                                Pose pose = result.getHitPose();
                                if (catchCheck(pose.tx(), pose.ty(), pose.tz(), "longTouch")) {
                                    Matrix.translateM(secondFlagMatrix, 0, 0f, borderPointY, 0f);
                                    mRenderer.mflag2.setModelMatrix(secondFlagMatrix);

                                    lineDraw();

                                    state = PLANET_DISTANCE_INFO;
                                    mCatched = false;
                                    messageFlag = true;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            text2.setText("2번 플래그 뜸");
                                        }
                                    });
                                    break;
                                }
                            }
                            longTouch = false;
                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    text2.setText("어림없지");
                                }
                            });
                            longTouch = false;
                        }
                    }
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text1.setText(state);
                        Log.d("플래그",messageFlag+", " + messageCount);
//                        while(messageFlag){
//                            Log.d("플래그 while문",state);
//                            comment.setText(getCommentMessage(state));
//                            comment.setVisibility(View.VISIBLE);
//
//                            messageCount++;
//
//                            if(messageCount == 15){
//                                Log.d("플래그 if문",state);
//                                messageFlag = false;
//                                comment.setVisibility(View.INVISIBLE);
//                                messageCount = 0;
//                            }
//                            SystemClock.sleep(100);
//                        }

                        comment.setText(getCommentMessage(state));

                        if(state.equals(RISING_SUN)){
                            btnLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });



            }
        });

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8,8,8,8,16,0);
        mSurfaceView.setRenderer(mRenderer);
    }

    boolean messageFlag = true;
    int messageCount=0;
    String getCommentMessage(String state){
        String res = "";
        switch (state){
            case INIT:
                res = "안녕! 난 안디라고해! \n 그림을 비추면 멋잇는 태양계가 나타날거야!";
                break;

            case IMAGE_SCAN:
                res = "이제 곧 태양이 떠오를 거야!!";
                break;

            case RISING_SUN:
                res = "태양계 행성들이야!! 멋잇지? 헤헷";
                break;

            case PLANET_MOVE_OFF:
                res = "행성들이 멈춰있네?\n" +
                        "시작 버튼을 누르면 행성이 움직일거야!";
                break;

            case PLANET_MOVE_ON:
                res = "돌아라 돌아라!!!\n" +
                        "멈추고 싶으면 정지 버튼을 눌러줘!";
                break;

            case PLANET_DISTANCE_ONE:
                res = "와! 귀여운 깃발이 꽂혔어!\n" +
                        "이 행성과 거리를 알고싶은 행성을 또 눌러줘";
                break;

            case PLANET_DISTANCE_INFO:
                int result = Math.abs(firstPlanet.distance-secondPlanet.distance);
                res = "\""+firstPlanet.name_kor+"\"과 \""+ secondPlanet.name_kor+"\"은 "+ result+"만 km 떨어져있어!!";
                break;

            case PLANET_REMOVE:
                res = "블랙홀이 태양계를 집어삼키고 있어!!!!!";

        }

        return res;
    }

    void lineDraw(){
        mRenderer.lineDraw = true;
        mRenderer.addLine(firstPlanetCenter[0],firstPlanetCenter[1],firstPlanetCenter[2], lineModelMatrix);

        mRenderer.addLine(firstPlanetCenter[0],firstPlanetCenter[1],firstPlanetCenter[2]);


        mRenderer.addPoint2(firstPlanetCenter[0],firstPlanetCenter[1],firstPlanetCenter[2]);
        mRenderer.addPoint2(secondPlanetCenter[0],secondPlanetCenter[1],secondPlanetCenter[2]);
//        int a = 10;
//        float dx = (secondPlanetCenter[0]-firstPlanetCenter[0])/a;
//        float dy = (secondPlanetCenter[1]-firstPlanetCenter[1])/a;
//        float dz = (secondPlanetCenter[2]-firstPlanetCenter[2])/a;
//
//        Log.d("선 생성 >>>>>>> " ,"x증감 : " + dx + " y증감 : " + dy + " z증감 : " + dz);
//
//        float fx = firstPlanetCenter[0];
//        float fy = firstPlanetCenter[1];
//        float fz = firstPlanetCenter[2];
//
//        for (int i = 0; i<=a; i++){
//            Log.d("선 생성 ", i +"번째 ==> " + "x : " + fx + " y : " + fy + " z : " + fz);
//            mRenderer.addPoint(fx,fy,fz);
//            mRenderer.addPoint2(fx,fy,fz);
//            fx += dx;
//            fy += dy;
//            fz += dz;
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestCameraPermission();
        try {
            if(mSession == null){
                switch(ArCoreApk.getInstance().requestInstall(this, true)){
                    case INSTALLED:
                        mSession = new Session(this);
                        Log.d("메인", "ARCore session 생성");
                        break;
                    case INSTALL_REQUESTED:
                        Log.d("메인","ARCore 설치 필요");
                        mUserRequestedInstall = false;
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mConfig = new Config(mSession);

        mConfig.setFocusMode(Config.FocusMode.AUTO);
        // 이미지데이터베이스 설정
        setUpImgDB(mConfig);

        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        mSurfaceView.onResume();
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    // 이미지데이터베이스 설정
    void setUpImgDB(Config config){
        // 이미지 데이터베이스 생성
        AugmentedImageDatabase imageDatabase = new AugmentedImageDatabase(mSession);

        try {
            // 파일스트림로드
            InputStream is = getAssets().open("solarsystem.png");
            // 파일스트림에서 Bitmap 생성
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            // 이미지데이터베이스에 bitmap 추가
            imageDatabase.addImage("태양계",bitmap);

            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        config.setAugmentedImageDatabase(imageDatabase);
    }

    float[] sunMatrix = new float[16];
    float[] imageMatrix = new float[16];
    // 이미지추적결과에 따른 그리기 설정
    void drawImage(Frame frame){

        mRenderer.isImgFind = false;
        // frame(카메라) 에서 찾은 이미지들을 Collection으로 받아온다.
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        // 찾은 이미지들을 돌린다.
        for (AugmentedImage img : updatedAugmentedImages) {
            if (img.getTrackingState() == TrackingState.TRACKING) {
                mRenderer.isImgFind = true;
                state = IMAGE_SCAN;
                messageFlag = true;
                Pose imgPose = img.getCenterPose();

                float[] matrix = new float[16];
                imgPose.toMatrix(matrix, 0);

                System.arraycopy(matrix, 0, imageMatrix, 0 ,16);

                mRenderer.mCube.setModelMatrix(imageMatrix);
                moveObj(matrix);
                drawTag = true;

                switch (img.getTrackingMethod()) {
                    case LAST_KNOWN_POSE:
                        break;
                    case FULL_TRACKING:
                        break;
                    case NOT_TRACKING:
                        break;
                }
            }
        }
    }

    boolean initStop = false;
    boolean drawTag =false;

    int endTime = 3700;


    void moveObj(float[] matrix) {
        System.arraycopy(matrix, 0, sunMatrix, 0, 16);
        Matrix.translateM(sunMatrix, 0, 0f, -0.7f, 0f);

        Log.d("sunMatrix 처음", Arrays.toString(sunMatrix));
        new Thread() {
            @Override
            public void run() {
                int currentTime = 0;
                while (!initStop) {
                    if(currentTime<100) {
                        drawTag = true;
                        Matrix.translateM(sunMatrix, 0, 0f, 0.01f, 0f);
                        mRenderer.sun.setModelMatrix(sunMatrix);
                        SystemClock.sleep(100);
                    }else if( currentTime == 100){
                        mRenderer.planetDraw = true;
                        state = RISING_SUN;
                        messageFlag = true;
                    }
                    else if (currentTime<endTime){
                        if(mRenderer.planetDraw){
                            for(Planet planet : mRenderer.planetList){
                                planet.initPlanet(sunMatrix,currentTime,endTime);
                            }

                            SystemClock.sleep(1);
                        }
                    }
                    else if(currentTime > 3701){

                        state = PLANET_MOVE_OFF;
                        Matrix.translateM(mRenderer.moon.myMatrix,  0, mRenderer.earth.myMatrix,0, 0.1f*(float) Math.cos((revolutionAngle) * Math.PI / 180),0.1f *(float) Math.sin((revolutionAngle) * Math.PI / 180),0f);
                        mRenderer.moon.setModelMatrix(mRenderer.moon.myMatrix);
                        mRenderer.moonDraw = true;
                        messageFlag = true;
                        initStop = true;
                    }
                    currentTime++;
                }
            }
        }.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
        mSession.pause();

    }

    void hideStatusBarAndTitleBar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    void requestCameraPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    0
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);

        return true;
    }

    String name_eng; // 영어이름
    String name_kor; // 한국명
    String info_distance; // 지름
    String info_surface; // 표면적
    String info_weight; // 질량
    String info_lean; // 자전축 기울기
    String info_revolve; // 공전 주기
    String info_rotate; // 자전 주기
    String info_maxCel; // 최고섭씨온도
    String info_minCel; // 최저섭씨온도
    String info_press; // 대기압
    String info_satellite; // 위성

    Planet firstPlanet;
    Planet secondPlanet;

    boolean catchCheck(float x, float y, float z, String event) {
        for (Planet planet : mRenderer.planetList) {

            float[][] resAll = planet.getMinMaxPoint();
            float[] minPoint = resAll[0];
            float[] maxPoint = resAll[1];

            if (x >= minPoint[0] - 0.2f && x <= maxPoint[0] + 0.2f &&
                    y >= minPoint[1] - 0.1f && y <= maxPoint[1] + 0.1f &&
                    z >= minPoint[2] - 0.1f && z <= maxPoint[2] + 0.1f) {
                if(event.equals("longTouch")){
                    if (state.equals(PLANET_MOVE_OFF) || state.equals(PLANET_MOVE_ON)) {
                        firstPlanet = planet;
                        firstPlanetCenter = objCentCoord(maxPoint, minPoint);

                        System.arraycopy(planet.myMatrix, 0, firstFlagMatrix, 0, 16);
                        Matrix.translateM(firstFlagMatrix, 0, 0f, 0.15f, 0f);
                        System.arraycopy(planet.myMatrix, 0, lineModelMatrix, 0, 16);
                        return true;
                    } else if (state.equals(PLANET_DISTANCE_ONE)) {
                        if (!planet.name.equals(firstPlanet.name)) {
                            secondPlanet = planet;
                            secondPlanetCenter = objCentCoord(maxPoint, minPoint);

                            System.arraycopy(planet.myMatrix, 0, secondFlagMatrix, 0, 16);

                            Matrix.translateM(secondFlagMatrix, 0, 0f, 0.15f, 0f);
                            return true;
                        }
                    }
                }
                else if(event.equals("doubleTap")){
                    name_eng = planet.name;
                    name_kor = planet.name_kor;
                    info_distance = planet.info_distance;
                    info_surface = planet.info_surface;
                    info_weight = planet.info_weight;
                    info_lean = planet.info_lean;
                    info_revolve = planet.info_revolve;
                    info_rotate = planet.info_rotate;
                    info_maxCel = planet.info_maxCel;
                    info_minCel = planet.info_minCel;
                    info_press = planet.info_press;
                    info_satellite = planet.info_satellite;

                    return true;
                }
            }
        }
        return false;
    }

    float[] objCentCoord(float[] maxPoint, float[] minPoint){

        float rdsX = ((maxPoint[0]+minPoint[0])/2);
        float rdsY = ((maxPoint[1]+minPoint[1])/2);
        float rdsZ = ((maxPoint[2]+minPoint[2])/2);

        float[] res = new float[]{rdsX, rdsY, rdsZ};
        return res;
    }

    boolean rotateFlag = false;
    boolean blackHoleFlag = false;
    double sunAngle = 0;
    double revolutionAngle = 0;
    float rotateAngle = 0;
    float[] calculateInitialMePoint(int width, int height,
                                    float[] projMat, float[] viewMat) {
        return getScreenPoint(width-100f, height-80f, width, height, projMat, viewMat);
    }
    //평면화
    public float[] getScreenPoint(float x, float y, float w, float h,
                                  float[] projMat, float[] viewMat) {
        float[] position = new float[3];
        float[] direction = new float[3];

        x = x * 2 / w - 1.0f;
        y = (h - y) * 2 / h - 1.0f;

        float[] viewProjMat = new float[16];
        Matrix.multiplyMM(viewProjMat, 0, projMat, 0, viewMat, 0);

        float[] invertedMat = new float[16];
        Matrix.setIdentityM(invertedMat, 0);
        Matrix.invertM(invertedMat, 0, viewProjMat, 0);

        float[] farScreenPoint = new float[]{x, y, 1.0F, 1.0F};
        float[] nearScreenPoint = new float[]{x, y, -1.0F, 1.0F};
        float[] nearPlanePoint = new float[4];
        float[] farPlanePoint = new float[4];

        Matrix.multiplyMV(nearPlanePoint, 0, invertedMat, 0, nearScreenPoint, 0);
        Matrix.multiplyMV(farPlanePoint, 0, invertedMat, 0, farScreenPoint, 0);

        position[0] = nearPlanePoint[0] / nearPlanePoint[3];
        position[1] = nearPlanePoint[1] / nearPlanePoint[3];
        position[2] = nearPlanePoint[2] / nearPlanePoint[3];

        direction[0] = farPlanePoint[0] / farPlanePoint[3] - position[0];
        direction[1] = farPlanePoint[1] / farPlanePoint[3] - position[1];
        direction[2] = farPlanePoint[2] / farPlanePoint[3] - position[2];

        normalize(direction);

        position[0] += (direction[0] * 0.1f);
        position[1] += (direction[1] * 0.1f);
        position[2] += (direction[2] * 0.1f);

        return position;
    }

    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

    public void rotateThreadOn(){
        new Thread() {
            @Override
            public void run() {
                while (rotateFlag) {
                    sunAngle += 0.01;
                    revolutionAngle += 1;
                    rotateAngle += 1;
                    Matrix.rotateM(sunMatrix, 0, 0.01f, 0f, 1f, 0f);

                    for (Planet planet : mRenderer.planetList) {
                        planet.movePlanet(sunMatrix, sunAngle, revolutionAngle, rotateAngle);
                    }

                    Matrix.translateM(mRenderer.moon.myMatrix, 0, mRenderer.earth.myMatrix, 0, 0.1f * (float) Math.cos((revolutionAngle) * Math.PI / 180), 0.1f * (float) Math.sin((revolutionAngle) * Math.PI / 180), 0f);

                    mRenderer.sun.setModelMatrix(sunMatrix);
                    mRenderer.moon.setModelMatrix(mRenderer.moon.myMatrix);

                    SystemClock.sleep(50);
                }

            }
        }.start();
    }

    float[] blackHoleMatrix = new float[16];

    public void rotateBtnClick(View view) {

        if (view.getId() == R.id.rotateBtn) {
            if (state.equals(PLANET_MOVE_OFF)) {
                rotateFlag = true;
                state = PLANET_MOVE_ON;
                messageFlag = true;
                rotateThreadOn();
            }
        }

        else if(view.getId() == R.id.rotateStopBtn) {
            rotateFlag = false;
            state = PLANET_MOVE_OFF;
            messageFlag = true;
        }

        else if(view.getId() == R.id.initBtn) {
            if (state.equals(PLANET_MOVE_ON) || state.equals(PLANET_MOVE_OFF)) {
                rotateFlag = false;
                blackHoleFlag = true;
                mRenderer.moonDraw = false;
                state = PLANET_REMOVE;
                messageFlag = true;

                mRenderer.blackholeDraw = true;
                System.arraycopy(imageMatrix, 0, blackHoleMatrix, 0, 16);
                Matrix.translateM(blackHoleMatrix, 0, 0f, 0.01f, 0f);

                new Thread() {
                    @Override
                    public void run() {
                        int currentTime = 100;
                        while (blackHoleFlag) {
                            if (currentTime < endTime) {

                                for (Planet planet : mRenderer.planetList) {
                                    planet.removePlanet(sunMatrix, currentTime, endTime, sunAngle, revolutionAngle);
                                }
                                SystemClock.sleep(1);
                            } else if (currentTime == endTime) {
                                mRenderer.planetDraw = false;
                            } else if (currentTime < 3780) {
                                Matrix.translateM(sunMatrix, 0, 0f, -0.01f, 0f);
                                mRenderer.sun.setModelMatrix(sunMatrix);
                                SystemClock.sleep(50);
                            } else if (currentTime == 3780) {
                                state = INIT;
                                mRenderer.isImgFind = false;
                                drawTag = false;
                                mRenderer.blackholeDraw = false;
                                blackHoleFlag = false;
                                messageFlag = true;
                            }
                            Matrix.rotateM(blackHoleMatrix,0,4,0,1f,0);
                            mRenderer.blackhole.setModelMatrix(blackHoleMatrix);
                            currentTime++;
                        }
                    }
                }.start();
            }
        }
    }


    public void flagBtnClick(View view){
        if(view.getId() == R.id.pengBtn){
            if (state.equals(PLANET_DISTANCE_INFO)) {
                mRenderer.objChanged(0, firstFlagMatrix, secondFlagMatrix);
            }else if (state.equals(PLANET_DISTANCE_ONE)){
                mRenderer.objChanged(0, firstFlagMatrix, null);
            }
        }

        else if(view.getId() == R.id.andyBtn){
            if (state.equals(PLANET_DISTANCE_INFO)) {
                mRenderer.objChanged(1, firstFlagMatrix, secondFlagMatrix);
            }else if (state.equals(PLANET_DISTANCE_ONE)){
                mRenderer.objChanged(1, firstFlagMatrix, null);
            }
        }

        else if(view.getId() == R.id.flagResetBtn){
            if(state.equals(PLANET_DISTANCE_ONE)||state.equals(PLANET_DISTANCE_INFO)){
                state = prevState;
                mRenderer.flagDraw = false;
                firstFlagMatrix = new float[16];
                secondFlagMatrix = new float[16];
                lineModelMatrix = new float[16];
                mRenderer.mPaths = new ArrayList<>();
                mRenderer.line.mPoint =  new float[mRenderer.line.maxPoints * 3];
                mRenderer.line.mNumPoints = 0;
                if(state.equals(PLANET_MOVE_ON)){
                    rotateFlag = true;
                    rotateThreadOn();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        flagBtnLayout.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }
    }
}
