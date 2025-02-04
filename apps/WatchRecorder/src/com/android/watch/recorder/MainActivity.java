package com.android.watch.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
public class MainActivity extends Activity{
	public static int startCount=1;
	public static int second=0;
	public static int minute=0;
	//通知栏用
	NotificationManager notificationManager;
	/**文件存在**/
	public static boolean sdcardExit;
	public static File myRecAudioFile;
	//**是否暂停标志位**/
	public static boolean isPause; 
	/**是否又重新再录一次**/
	public static boolean isReStart;
	/**录音保存路径**/
	public static File myRecAudioDir;
	private  final String SUFFIX=".amr";
	/**是否停止录音**/
	private boolean isStopRecord;
	/**记录需要合成的几段amr语音文件**/
	public static ArrayList<String> lists;
	private ArrayList<String> listTimes;
	public static Map map;
	/**存放音频文件列表**/
	public static ArrayList<String> recordFiles;
	public static ArrayList<Item> recordFile;
	private ArrayAdapter<String> adapter;
	public static MediaRecorder mMediaRecorder;
	MediaPlayer mediaPlayer;
	private String length1 = null;
    public static ImageView imageView;
    ImageView menu;
    public static TextView times;
    public static NotificationCompat.Builder builder;
    public static ImageView cancel;
    public static ImageView save;
//    Button cancel;
//    Button save;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recorder_main);
		MyClick myClick=new MyClick(); 
		mediaPlayer = new MediaPlayer();
		lists=new ArrayList<String>();
		map=new HashMap();
		notificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		listTimes=new ArrayList<String>();
		imageView=(ImageView) this.findViewById(R.id.recorder);
		imageView.setOnClickListener(myClick);
		times=(TextView) this.findViewById(R.id.times);
		cancel=(ImageView) this.findViewById(R.id.cancel);
		save=(ImageView) this.findViewById(R.id.save);
		menu=(ImageView) this.findViewById(R.id.menu);
		menu.setOnClickListener(myClick);
		cancel.setOnClickListener(myClick);
		save.setOnClickListener(myClick);
		isPause=false;
		isReStart=false;
		// 判断sd Card是否插入
		sdcardExit = Environment.getExternalStorageState().equals(
						android.os.Environment.MEDIA_MOUNTED);
				// 取得sd card路径作为录音文件的位置
				if (sdcardExit){
					String pathStr = Environment.getExternalStorageDirectory().getAbsolutePath()+"/YYT";
					//String pathStr = "/storage/sdcard1/MIUI/"+"/YY";
					myRecAudioDir= new File(pathStr);
					if(!myRecAudioDir.exists()){
						myRecAudioDir.mkdirs();
						Log.v("录音", "创建录音文件！" + myRecAudioDir.exists());
					}
//					Environment.getExternalStorageDirectory().getPath() + "/" + PREFIX + "/";
				}
				// 取得sd card 目录里的.arm文件
				getRecordFiles();
//				map.put("recordFiles", recordFiles);
//				map.put("listTimes", listTimes);
//				adapter = new ArrayAdapter<String>(this,
//						android.R.layout.simple_list_item_1, recordFiles);
	}
	
	class MyClick implements View.OnClickListener{
		Intent intent2=null;
		@Override
		public void onClick(View arg0) {
			switch (arg0.getId()) {
			case R.id.recorder:
				intent2 = new Intent(MainActivity.this, RecService.class);
				if(isReStart){
					lists.clear();
				}
//				if(isPause){
//					//录音状态要转为暂停状态
//					imageView.setImageResource(R.drawable.startrecorder);
//					lists.add(myRecAudioFile.getPath());
//					recorderStop();
//				//	start();
//					//buttonpause.setText("继续录音");
//					//计时停止
//					timer.cancel();
//					isPause=false; 
//					isReStart=false;
//				}
				//开始状态要录音
//				else{
					startService(intent2);
//					imageView.setImageResource(R.drawable.endre);
//					start();
//					isPause=true;
//				}
				startCount++;
				break;
			case R.id.save:
				//timer.cancel();
				//这里写暂停处理的 文件！加上list里面 语音合成起来
				if(!isPause){
					//在暂停状态按下结束键,处理list就可以了
					getInputCollection(lists, false);
					isPause=true;
				//	adapter.add(myRecAudioFile.getName());
				}
				else{
					lists.add(myRecAudioFile.getPath());
					recorderStop();
					getInputCollection(lists, true);
					 
				}
				minute=0;
				second=0;
				times.setText("00:00");
				getRecordFiles();
				isStopRecord = true;
				isReStart=true;
				imageView.setImageResource(R.drawable.startrecorder);
				save.setVisibility(View.INVISIBLE);
				cancel.setVisibility(View.INVISIBLE);
				isPause=false;
				if(timer!=null){
					timer.cancel();
				}
				notificationManager.cancel(1);
				break;
			case R.id.cancel:
				recorderStop();
				deleteListRecord(isPause);
				minute=0;
				second=0;
				times.setText("00:00");
				imageView.setImageResource(R.drawable.startrecorder);
				save.setVisibility(View.INVISIBLE);
				cancel.setVisibility(View.INVISIBLE);
				isPause=false;
				isReStart=true;
				notificationManager.cancel(1);
				break;
			case R.id.menu:
				Intent intent=new Intent(MainActivity.this, HistoryListActivity.class);
				intent.putExtra("recordFiles", recordFiles);
				startActivity(intent);
				break;
			default:
				break;
			}
		}
	}
//	/**计时器**/
	public static Timer timer;
//	public void start() {
//		 TimerTask timerTask=new TimerTask() {
//			@Override
//			public void run() {
//				second++;
//				if(second>=60){
//					second=0;
//					minute++;
//				}
//				handler.sendEmptyMessage(0);
//			}
//		};
//		 timer=new Timer();
//		 timer.schedule(timerTask, 0,1000);
//		try {
//			if (!sdcardExit) {
//				Toast.makeText(MainActivity.this, "请插入SD card",
//						Toast.LENGTH_LONG).show();
//				return;
//			}
//			String mMinute1=getTime();
//			// 创建音频文件
////			myRecAudioFile = File.createTempFile(mMinute1, ".amr",
////					myRecAudioDir);
//			myRecAudioFile=new File(myRecAudioDir,mMinute1+SUFFIX);
//			mMediaRecorder = new MediaRecorder();
//			// 设置录音为麦克风
//			mMediaRecorder
//					.setAudioSource(MediaRecorder.AudioSource.MIC);
//			mMediaRecorder
//					.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
//			mMediaRecorder
//					.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
////			mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);     
////			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//			//录音文件保存这里
//			mMediaRecorder.setOutputFile(myRecAudioFile
//					.getAbsolutePath());
//			mMediaRecorder.prepare();
//			mMediaRecorder.start();
//			save.setVisibility(View.VISIBLE);
//			cancel.setVisibility(View.VISIBLE);
//			isStopRecord = false;
//		} catch (IOException e) {
//			e.printStackTrace();
//
//		}
//	}
	public void panding() {
		builder = new NotificationCompat.Builder(this);
		// builder对象在构造通知对象之前，做一些通知对象的设置
		// 小图标设置
		builder.setSmallIcon(R.drawable.recordermain);
		// 把资源转换成Bitmap对象
		Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.recordermain);
		// 设置大图标
		builder.setLargeIcon(bitmap);
		// 设置通知子描述
		//builder.setSubText("SubText");
		// 通知消息
		//builder.setNumber(9);
		// 通知标题
		builder.setContentTitle("录音机");
		// 设置通知子描述
		builder.setContentText(times.getText());
		// 设置进行中通知
		builder.setOngoing(true);
		//构造一个PendingIntent
//		Intent intent=new Intent(this, MainActivity.class);
		Intent intent=new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(new ComponentName(this.getPackageName(), this.getPackageName() + "." + this.getLocalClassName())); 
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);//关键的一步，设置启动模式
		//这里需要使用PendingIntent.FLAG_UPDATE_CURRENT来覆盖以前已经存储在的PendingIntent
		PendingIntent pendingIntent=PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		//设置setContentIntent
		builder.setContentIntent(pendingIntent);
		//点击执行了PendingIntent之后，通知自动销毁
//		builder.setAutoCancel(true);
		// 构造通知对象
		Notification notification = builder.build();
		// 发布通知，通知ID为1
		notificationManager.notify(1, notification);
	}
//	Handler handler=new Handler(){
//		@Override
//		public void handleMessage(Message msg) {
//			super.handleMessage(msg);
//			String minutes = null,seconds=null;
//			if(minute<10){
//				minutes="0"+minute;
//			}else{
//				minutes=minute+"";	
//			}
//			if(second<10){
//				seconds="0"+second;
//			}else{
//				seconds=second+"";
//			}
//			panding();
//			times.setText(minutes+":"+seconds);
//		}
//	};
	private String getTime(){
		SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyyMMddHHmmss");      
		Date  curDate=new  Date(System.currentTimeMillis());//获取当前时间      
		String   time   =   formatter.format(curDate);  
		System.out.println("当前时间");
		return time;
		}
	protected void recorderStop() {
		if (mMediaRecorder != null) {
			// 停止录音
			//mMediaRecorder.stop();
			mMediaRecorder.release();
			mMediaRecorder = null;
			timer.cancel();
		}
		
	}
	/**
	 *  @param isAddLastRecord 是否需要添加list之外的最新录音，一起合并
	 *  @return 将合并的流用字符保存
	 */
	public  void getInputCollection(List list,boolean isAddLastRecord){
		String	mMinute1=getTime();
		// 创建音频文件,合并的文件放这里
		File file1=new File(myRecAudioDir,mMinute1+SUFFIX);
		FileOutputStream fileOutputStream = null;
		if(!file1.exists()){
			try {
				file1.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			fileOutputStream=new FileOutputStream(file1);

		} catch (IOException e) {
			e.printStackTrace();
		}
		//list里面为暂停录音 所产生的 几段录音文件的名字，中间几段文件的减去前面的6个字节头文件
		for(int i=0;i<list.size();i++){
			File file=new File((String) list.get(i));
			try {
				FileInputStream fileInputStream=new FileInputStream(file);
				byte  []myByte=new byte[fileInputStream.available()];
				//文件长度
				int length = myByte.length;
				//头文件
				if(i==0){
						while(fileInputStream.read(myByte)!=-1){
								fileOutputStream.write(myByte, 0,length);
							}
						}
				//之后的文件，去掉头文件就可以了
				else{
					while(fileInputStream.read(myByte)!=-1){
						
						fileOutputStream.write(myByte, 6, length-6);
					}
				}
				fileOutputStream.flush();
				fileInputStream.close();
				System.out.println("合成文件长度："+file1.length());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			}
		//结束后关闭流
		try {
			fileOutputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			//合成一个文件后，删除之前暂停录音所保存的零碎合成文件
			deleteListRecord(isAddLastRecord);
	}
	private void deleteListRecord(boolean isAddLastRecord){
		for(int i=0;i<lists.size();i++){
			File file=new File((String) lists.get(i));
			if(file.exists()){
				file.delete();
			}
		}
		//正在暂停后，继续录音的这一段音频文件
		if(isAddLastRecord){
			myRecAudioFile.delete();
		}
	}
	
	
	@Override
	protected void onDestroy() {
		if(timer!=null){
			timer.cancel();
		}
		notificationManager.cancel(1);
		super.onDestroy();
		
	}
	/**
	 * 获取目录下的所有音频文件
	 */
	private void getRecordFiles() {
		recordFiles = new ArrayList<String>();
		recordFile = new ArrayList<Item>();
		if (sdcardExit) {
			File files[] = myRecAudioDir.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().indexOf(".") >= 0) { // 只取.amr 文件
						String fileS = files[i].getName().substring(
								files[i].getName().indexOf("."));
						if (fileS.toLowerCase().equals(".mp3")
								|| fileS.toLowerCase().equals(".amr")
								|| fileS.toLowerCase().equals(".mp4"))
							recordFiles.add(files[i].getName());
							try {
								mediaPlayer.setDataSource(files[i].getAbsolutePath());
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (SecurityException e) {
								e.printStackTrace();
							} catch (IllegalStateException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						recordFile.add(new Item(files[i].getName(), mediaPlayer.getDuration()+""));
					}
				}
			}
		}

	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(false);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
}
