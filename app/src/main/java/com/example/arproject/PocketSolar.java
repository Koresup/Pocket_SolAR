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
import java.util.Collection;
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
    TextView comment;

    float[] mePos = new float[3];

    //////////////////////////////////////////////////////////////////////////////////////
    ImageButton pengBtn, andyBtn;

    boolean mCatched = false;
    float mCatchX, mCatchY;
    float borderPointY;

    GestureDetector mGestureDetector;

    float[] firstFlagMatrix = new float[16];
    float[] secondFlagMatrix = new float[16];
    float[] lineModelMatrix = new float[16];

    Planet firstPlanet;
    Planet secondPlanet;

    ImageButton flagResetBtn;

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    LinearLayout layout_info;
    ImageButton info_cancel;
    TextView txt_korName, txt_engName, txt_distance, txt_surface, txt_weight, txt_lean,
            txt_revolve, txt_rotate, txt_maxCel, txt_minCel, txt_press, txt_satellite;

    boolean doubleTap = false;
    boolean longTouch = false;

    Planet infoPlanet;

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

                mRenderer.pika.setViewMatrix(viewMatrix);

                mePos = calculateInitialMePoint(
                        mRenderer.mViewportWidth,
                        mRenderer.mViewportHeight,
                        projMatrix,
                        viewMatrix
                );

                float[] modelMatrix = new float[16];
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix,0, mePos[0], mePos[1], mePos[2]);
                Matrix.scaleM(modelMatrix,0, 0.2f, 0.2f, 0.2f);
                mRenderer.pika.setModelMatrix(modelMatrix);

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

                                            txt_korName.setText(infoPlanet.name_kor);
                                            txt_engName.setText(infoPlanet.name);
                                            txt_distance.setText(infoPlanet.info_distance);
                                            txt_surface.setText(infoPlanet.info_surface);
                                            txt_weight.setText(infoPlanet.info_weight);
                                            txt_lean.setText(infoPlanet.info_lean);
                                            txt_revolve.setText(infoPlanet.info_revolve);
                                            txt_rotate.setText(infoPlanet.info_rotate);
                                            txt_maxCel.setText(infoPlanet.info_maxCel);
                                            txt_minCel.setText(infoPlanet.info_minCel);
                                            txt_press.setText(infoPlanet.info_press);
                                            txt_satellite.setText(infoPlanet.info_satellite);
                                            layout_info.setVisibility(View.VISIBLE);
                                            // 정보 설정
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
                                    mRenderer.firstFlag =true;
                                    rotateFlag = false;
                                    messageFlag = true;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            flagBtnLayout.setVisibility(View.VISIBLE);
                                        }
                                    });
                                    break;
                                }
                            }
                        } else if (state.equals(PLANET_DISTANCE_ONE)){
                            for (HitResult result : results) {
                                Pose pose = result.getHitPose();
                                if (catchCheck(pose.tx(), pose.ty(), pose.tz(), "longTouch")) {
                                    Matrix.translateM(secondFlagMatrix, 0, 0f, borderPointY, 0f);
                                    mRenderer.mflag2.setModelMatrix(secondFlagMatrix);


                                    state = PLANET_DISTANCE_INFO;
                                    mRenderer.secondFlag =true;
                                    mCatched = false;
                                    messageFlag = true;
                                    break;
                                }
                            }
                        }

                        longTouch = false;
                    }
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(state.equals(PLANET_INFO)){
                            comment.setVisibility(View.INVISIBLE);
                        }else{
                            comment.setVisibility(View.VISIBLE);
                        }
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
                res = "안녕! 난 피카츄라고해! \n 카메라에 그림을 비추면 \n태양계가 나타날거야!";
                break;

            case IMAGE_SCAN:
                res = "이제 곧 태양이 떠오를 거야!!";
                break;

            case RISING_SUN:
                res = "태양계 행성들이야!! 멋잇지? 헤헷";
                break;

            case PLANET_MOVE_OFF:
                res = "지금은 행성들이 움직이지 않아..\n" +
                        "시작을 눌러보면 행성이 움직일거야!";
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
                res = "블랙홀이 태양계를 집어삼키고 있어!!!!!\n다음에 또 만나!";

        }

        return res;
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
        setUpImgDB(mConfig);

        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        mRenderer.isImgFind = false;
        mSurfaceView.onResume();
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    void setUpImgDB(Config config){
        AugmentedImageDatabase imageDatabase = new AugmentedImageDatabase(mSession);

        try {
            InputStream is = getAssets().open("sunImage.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            imageDatabase.addImage("태양",bitmap);

            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        config.setAugmentedImageDatabase(imageDatabase);
    }

    float[] sunMatrix = new float[16];
    float[] imageMatrix = new float[16];
    void drawImage(Frame frame){

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage img : updatedAugmentedImages) {
            if (img.getTrackingState() == TrackingState.TRACKING) {

                state = IMAGE_SCAN;
                messageFlag = true;
                Pose imgPose = img.getCenterPose();

                float[] matrix = new float[16];
                imgPose.toMatrix(matrix, 0);

                System.arraycopy(matrix, 0, imageMatrix, 0 ,16);

                moveObj(matrix);

                mRenderer.isImgFind = true;
                mRenderer.cubeDraw = true;
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

        new Thread() {
            @Override
            public void run() {
                int currentTime = 0;
                while (!initStop) {
                    if(currentTime<100) {
                        drawTag = true;
                        mRenderer.sunDraw = true;
                        Matrix.translateM(sunMatrix, 0, 0f, 0.01f, 0f);
                        mRenderer.sun.setModelMatrix(sunMatrix);
                        SystemClock.sleep(100);
                    }else if( currentTime == 100){
                        mRenderer.planetDraw = true;
                        mRenderer.cubeDraw = false;
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



    boolean catchCheck(float x, float y, float z, String event) {
        for (Planet planet : mRenderer.planetList) {

            float[][] resAll = planet.getMinMaxPoint();
            float[] minPoint = resAll[0];
            float[] maxPoint = resAll[1];

            if (x >= minPoint[0] - 0.3f && x <= maxPoint[0] + 0.3f &&
                    y >= minPoint[1] - 0.3f && y <= maxPoint[1] + 0.3f &&
                    z >= minPoint[2] - 0.3f && z <= maxPoint[2] + 0.3f) {
                if(event.equals("longTouch")){
                    if (state.equals(PLANET_MOVE_OFF) || state.equals(PLANET_MOVE_ON)) {
                        firstPlanet = planet;

                        System.arraycopy(planet.myMatrix, 0, firstFlagMatrix, 0, 16);
                        Matrix.translateM(firstFlagMatrix, 0, 0f, 0.15f, 0f);

                        System.arraycopy(planet.myMatrix, 0, lineModelMatrix, 0, 16);
                        return true;
                    } else if (state.equals(PLANET_DISTANCE_ONE)) {
                        if (!planet.name.equals(firstPlanet.name)) {
                            secondPlanet = planet;
                            System.arraycopy(planet.myMatrix, 0, secondFlagMatrix, 0, 16);

                            Matrix.translateM(secondFlagMatrix, 0, 0f, 0.15f, 0f);
                            return true;
                        }
                    }
                }
                else if(event.equals("doubleTap")){
                    infoPlanet = planet;

                    return true;
                }
            }
        }
        return false;
    }

    boolean rotateFlag = false;
    boolean blackHoleFlag = false;
    double sunAngle = 0;
    double revolutionAngle = 0;
    float rotateAngle = 0;
    float[] calculateInitialMePoint(int width, int height,
                                    float[] projMat, float[] viewMat) {
        return getScreenPoint(width-100f, height-130f, width, height, projMat, viewMat);
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
    boolean init = false;
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
            if(state.equals(PLANET_MOVE_ON)) {
                rotateFlag = false;
                state = PLANET_MOVE_OFF;
                messageFlag = true;
            }
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
                                if(currentTime == 3750){
                                    mRenderer.sunDraw = false;
                                }
                            } else if (currentTime == 3780) {
                                finish();
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
                mRenderer.firstFlag = false;
                mRenderer.secondFlag = false;
                firstFlagMatrix = new float[16];
                secondFlagMatrix = new float[16];
                lineModelMatrix = new float[16];
                mRenderer.mPaths = new ArrayList<>();
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
