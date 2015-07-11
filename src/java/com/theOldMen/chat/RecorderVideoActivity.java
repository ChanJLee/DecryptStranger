/************************************************************
 *  * EaseMob CONFIDENTIAL
 * __________________
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of EaseMob Technologies.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from EaseMob Technologies.
 */
package com.theOldMen.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.theOldMen.Activity.R;
import com.theOldMen.util.Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RecorderVideoActivity extends Activity implements
		OnClickListener, SurfaceHolder.Callback, OnErrorListener,
		OnInfoListener {

	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static String CLASS_LABEL = "RecordActivity";
	//录像最支持10秒
	private final static int s_maxLength 	= 10000;
	////////////////////////////////////////////////////////////////////////////////////////////////

	private PowerManager.WakeLock m_wakeLock;
	private ImageView m_startButton;// 开始录制按钮
	private ImageView m_stopButton;// 停止录制按钮
	private MediaRecorder m_mediaRecorder;// 录制视频的类
	private VideoView m_videoView;// 显示视频的控件

	// 设置视频文件输出的路径
	String m_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
			"/" + System.currentTimeMillis() + ".3gp";

	// 录制的视频路径
	private Camera m_camera;
	// 预览的宽高
	private int m_previewWidth = 480;
	private int m_previewHeight = 480;
	private Chronometer m_chronometer;
	private int m_frontCamera = 0;// 0是后置摄像头，1是前置摄像头
	private Button m_switchButton;
	private SurfaceHolder m_surfaceHolder;
	int m_defaultVideoFrameRate = -1;
	ProgressBarDeterminate m_progreesBarDeterminate;
	int m_time = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////


	@Override
	@SuppressWarnings("deprecated")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 去掉标题栏
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// 设置全屏
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// 选择支持半透明模式，在有surfaceview的activity中使用
		getWindow().setFormat(PixelFormat.TRANSLUCENT);

		setContentView(R.layout.recorder_activity);

		//防止休眠
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		m_wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
				CLASS_LABEL);
		m_wakeLock.acquire();

		//初始化窗口部件
		initViews();
	}

	@SuppressWarnings("deprecated")
	@SuppressLint("NewApi")
	private void initViews() {

		m_switchButton 		= (Button) findViewById(R.id.switch_btn);
		m_switchButton.setOnClickListener(this);
		m_switchButton.setVisibility(View.VISIBLE);
		m_videoView 		= (VideoView) findViewById(R.id.mVideoView);
		m_startButton 		= (ImageView) findViewById(R.id.recorder_start);
		m_stopButton 		= (ImageView) findViewById(R.id.recorder_stop);
		m_startButton.setOnClickListener(this);
		m_stopButton.setOnClickListener(this);
		m_surfaceHolder 	= m_videoView.getHolder();
		m_surfaceHolder.addCallback(this);
		m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		m_chronometer 		= (Chronometer) findViewById(R.id.chronometer);
		m_progreesBarDeterminate = (ProgressBarDeterminate) findViewById(R.id.progressDeterminate);
		m_progreesBarDeterminate.setMax(100);

		findViewById(R.id.m_recorderBack).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	public void back(View view) {

		releaseRecorder();
		releaseCamera();
		finish();
	}

	@Override
	@SuppressWarnings("deprecated")
	protected void onResume() {
		super.onResume();

		if (m_wakeLock == null) {

			// 获取唤醒锁,保持屏幕常亮
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			m_wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
					CLASS_LABEL);
			m_wakeLock.acquire();
		}
		if (!initCamera()) {
			showFailDialog();
		}
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecated")
	private boolean initCamera() {
		try {
			if (m_frontCamera == 0) {
				m_camera = Camera.open(CameraInfo.CAMERA_FACING_BACK);
			} else {
				m_camera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
			}

			m_camera.lock();
			m_surfaceHolder = m_videoView.getHolder();
			m_surfaceHolder.addCallback(this);
			m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			m_camera.setDisplayOrientation(90);
		} catch (RuntimeException ex) {
			return false;
		}
		return true;
	}

	private void handleSurfaceChanged() {

		if (m_camera == null) {
			finish();
			return;
		}

		boolean hasSupportRate = false;

		List<Integer> supportedPreviewFrameRates = m_camera.getParameters()
				.getSupportedPreviewFrameRates();
		if (supportedPreviewFrameRates != null
				&& supportedPreviewFrameRates.size() > 0) {
			Collections.sort(supportedPreviewFrameRates);
			for (int i = 0; i < supportedPreviewFrameRates.size(); i++) {
				int supportRate = supportedPreviewFrameRates.get(i);

				if (supportRate == 15) {
					hasSupportRate = true;
				}

			}
			if (hasSupportRate) {
				m_defaultVideoFrameRate = 15;
			} else {
				m_defaultVideoFrameRate = supportedPreviewFrameRates.get(0);
			}

		}

		// 获取摄像头的所有支持的分辨率
		List<Size> resolutionList = Utils.getResolutionList(m_camera);

		if (resolutionList != null &&
				resolutionList.size() > 0) {

			Collections.sort(resolutionList, new Utils.ResolutionComparator());
			Size previewSize = null;

			boolean hasSize = false;
			// 如果摄像头支持640*480，那么强制设为640*480
			for (int i = 0; i < resolutionList.size(); i++) {
				Size size = resolutionList.get(i);
				if (size != null && size.width == 640 && size.height == 480) {
					previewSize = size;
					m_previewWidth = previewSize.width;
					m_previewHeight = previewSize.height;
					hasSize = true;
					break;
				}
			}

			// 如果不支持设为中间的那个
			if (!hasSize) {
				int mediumResolution = resolutionList.size() / 2;
				if (mediumResolution >= resolutionList.size())
					mediumResolution = resolutionList.size() - 1;
				previewSize = resolutionList.get(mediumResolution);
				m_previewWidth = previewSize.width;
				m_previewHeight = previewSize.height;
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (m_wakeLock != null) {
			m_wakeLock.release();
			m_wakeLock = null;
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.switch_btn:
				switchCamera();
				break;
			case R.id.recorder_start:
				// start recording
				startRecording();
				Toast.makeText(this, "录像开始", Toast.LENGTH_SHORT).show();
				m_switchButton.setVisibility(View.INVISIBLE);
				m_startButton.setVisibility(View.INVISIBLE);
				m_stopButton.setVisibility(View.VISIBLE);

				// 重置其他
				m_chronometer.setBase(SystemClock.elapsedRealtime());
				m_chronometer.start();

				m_chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {

					@Override
					public void onChronometerTick(Chronometer chronometer) {
						m_progreesBarDeterminate.setProgress((++m_time) * 10);
					}
				});

				break;
			case R.id.recorder_stop:

				// 停止拍摄
				stopRecording();
				m_switchButton.setVisibility(View.VISIBLE);
				m_chronometer.stop();
				m_startButton.setVisibility(View.VISIBLE);
				m_stopButton.setVisibility(View.INVISIBLE);
				new AlertDialog.Builder(this)
						.setMessage("是否发送？")
						.setPositiveButton(R.string.ok,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
														int which) {
										//关闭DIALOG
										dialog.dismiss();

										Intent x = new Intent();
										x.putExtra("uri", m_path);

										RecorderVideoActivity.this.setResult(RESULT_OK, x);
										finish();
									}
								})
						.setNegativeButton(R.string.cancel,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
														int which) {
										dialog.dismiss();
										if (m_camera == null) {
											initCamera();
										}
										try {
											m_camera.setPreviewDisplay(m_surfaceHolder);
											m_camera.startPreview();
											handleSurfaceChanged();
										} catch (IOException e1) {}
									}
								}).setCancelable(false).show();
				break;

			default:
				break;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
							   int height) {
		// 将holder，这个holder为开始在oncreat里面取得的holder，将它赋给surfaceHolder
		m_surfaceHolder = holder;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (m_camera == null)
			initCamera();
		try {
			m_camera.setPreviewDisplay(m_surfaceHolder);
			m_camera.startPreview();
			handleSurfaceChanged();
		} catch (IOException e1) {}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {

		// surfaceDestroyed的时候同时对象设置为null
		releaseCamera();
	}

	public void startRecording() {
		if (m_mediaRecorder == null)
			initRecorder();

		m_mediaRecorder.setOnInfoListener(this);
		m_mediaRecorder.setOnErrorListener(this);
		m_mediaRecorder.start();
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecated")
	private void initRecorder() {

		//初始化相机
		if (m_camera == null) {
			initCamera();
		}

		//设置视频展示窗口
		m_videoView.setVisibility(View.VISIBLE);
		// TODO init button

		//暂停预览
		m_camera.stopPreview();
		m_mediaRecorder = new MediaRecorder();
		m_camera.unlock();
		m_mediaRecorder.setCamera(m_camera);
		m_mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

		// 设置录制视频源为Camera（相机）
		m_mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		if (m_frontCamera == 1) {
			m_mediaRecorder.setOrientationHint(270);
		} else {
			m_mediaRecorder.setOrientationHint(90);
		}

		// 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
		m_mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		m_mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

		// 设置录制的视频编码h263 h264
		m_mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		// 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
		m_mediaRecorder.setVideoSize(m_previewWidth, m_previewHeight);
		// 设置视频的比特率
		m_mediaRecorder.setVideoEncodingBitRate(384 * 1024);
		// // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
		if (m_defaultVideoFrameRate != -1) {
			m_mediaRecorder.setVideoFrameRate(m_defaultVideoFrameRate);
		}

		// 设置视频文件输出的路径
		m_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
				"/" + System.currentTimeMillis() + ".3gp";

		m_mediaRecorder.setOutputFile(m_path);
		m_mediaRecorder.setMaxDuration(s_maxLength);
		m_mediaRecorder.setPreviewDisplay(m_surfaceHolder.getSurface());
		try {
			m_mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void stopRecording() {
		if (m_mediaRecorder != null) {
			m_mediaRecorder.setOnErrorListener(null);
			m_mediaRecorder.setOnInfoListener(null);
			try {
				m_mediaRecorder.stop();
			} catch (IllegalStateException e) {}
		}
		releaseRecorder();

		if (m_camera != null) {
			m_camera.stopPreview();
			releaseCamera();
		}
	}

	private void releaseRecorder() {
		if (m_mediaRecorder != null) {
			m_mediaRecorder.release();
			m_mediaRecorder = null;
		}
	}

	protected void releaseCamera() {
		try {
			if (m_camera != null) {
				m_camera.stopPreview();
				m_camera.release();
				m_camera = null;
			}
		} catch (Exception e) {
		}
	}

	@SuppressLint("NewApi")
	public void switchCamera() {

		if (m_camera == null) {
			return;
		}
		if (Camera.getNumberOfCameras() >= 2) {
			m_switchButton.setEnabled(false);
			if (m_camera != null) {
				m_camera.stopPreview();
				m_camera.release();
				m_camera = null;
			}

			switch (m_frontCamera) {
				case 0:
					m_camera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
					m_frontCamera = 1;
					break;
				case 1:
					m_camera = Camera.open(CameraInfo.CAMERA_FACING_BACK);
					m_frontCamera = 0;
					break;
			}
			try {
				m_camera.lock();
				m_camera.setDisplayOrientation(90);
				m_camera.setPreviewDisplay(m_videoView.getHolder());
				m_camera.startPreview();
			} catch (IOException e) {
				m_camera.release();
				m_camera = null;
			}
			m_switchButton.setEnabled(true);

		}
	}

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {

		if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {

			stopRecording();
			m_switchButton.setVisibility(View.VISIBLE);
			m_chronometer.stop();
			m_startButton.setVisibility(View.VISIBLE);
			m_stopButton.setVisibility(View.INVISIBLE);
			m_chronometer.stop();
			if (m_path == null) {
				return;
			}
			new AlertDialog.Builder(this)
					.setMessage("是否发送？")
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface arg0,
													int arg1) {
									arg0.dismiss();

									Intent x = new Intent();
									x.putExtra("uri", m_path);

									RecorderVideoActivity.this.setResult(RESULT_OK, x);
									finish();
								}
							}).setNegativeButton(R.string.cancel, null)
					.setCancelable(false).show();
		}
	}

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {

		stopRecording();
		Toast.makeText(this,
				"Recording error has occurred. Stopping the recording",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseCamera();

		if (m_wakeLock != null) {
			m_wakeLock.release();
			m_wakeLock = null;
		}
	}

	@Override
	public void onBackPressed() {
		back(null);
	}

	private void showFailDialog() {
		new AlertDialog.Builder(this)
				.setTitle("提示")
				.setMessage("打开设备失败！")
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
												int which) {
								finish();
							}
						}).setCancelable(false).show();
	}
}
