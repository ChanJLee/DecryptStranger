package com.theOldMen.chat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.theOldMen.Activity.R;
import com.theOldMen.util.ImageCache;
import com.theOldMen.util.ImageResizer;
import com.theOldMen.util.Utils;
import com.theOldMen.util.VideoEntity;
import com.theOldMen.widget.RecyclingImageView;

import java.util.ArrayList;
import java.util.List;

public class ImageGridFragment extends Fragment implements OnItemClickListener {

	////////////////////////////////////////////////////////////////////////////////////////////////
	private int 			m_imageThumbSize		= -1;
	private int 			m_imageThumbSpacing		= -1;
	private ImageAdapter 	m_adapter;
	private ImageResizer 	m_imageResizer;
	List<VideoEntity> 		m_list;
	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public ImageGridFragment() {}
	////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//预览视图的大小
		m_imageThumbSize 	= getResources().getDimensionPixelSize(
				R.dimen.image_thumbnail_size);
		//预览视图中的分割距离
		m_imageThumbSpacing = getResources().getDimensionPixelSize(
				R.dimen.image_thumbnail_spacing);

		//存放视频的缩略图
		m_list 				= new ArrayList<VideoEntity>();

		//获取视频文件
		getVideoFile();

		//设置适配器
		m_adapter 			= new ImageAdapter(getActivity());

		//图像缓冲参数
		ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams();

		//设置内存缓冲为应用的 25%
		cacheParams.setMemCacheSizePercent(0.25f);


		// The ImageFetcher takes care of loading images into our ImageView
		// children asynchronously
		m_imageResizer = new ImageResizer(getActivity(), m_imageThumbSize);
		m_imageResizer.setLoadingImage(R.drawable.empty_photo);
		m_imageResizer.addImageCache(getActivity().getSupportFragmentManager(),
				cacheParams);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
							 ViewGroup container,  Bundle savedInstanceState) {

		final View v = inflater.inflate(R.layout.image_grid_fragment,
				container, false);
		final GridView mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(m_adapter);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView absListView,
											 int scrollState) {
				// Pause fetcher to ensure smoother scrolling when flinging
				if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
					// Before Honeycomb pause image loading on scroll to help
					// with performance
					if (!Utils.hasHoneycomb()) {
						m_imageResizer.setPauseWork(true);
					}
				} else {
					m_imageResizer.setPauseWork(false);
				}
			}

			@Override
			public void onScroll(AbsListView absListView, int firstVisibleItem,
								 int visibleItemCount, int totalItemCount) {
			}
		});

		// This listener is used to get the final width of the GridView and then
		// calculate the
		// number of columns and the width of each column. The width of each
		// column is variable
		// as the GridView has stretchMode=columnWidth. The column width is used
		// to set the height
		// of each view so we get nice square thumbnails.
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@TargetApi(VERSION_CODES.JELLY_BEAN)
					@Override
					public void onGlobalLayout() {
						final int numColumns = (int) Math.floor(mGridView
								.getWidth()
								/ (m_imageThumbSize + m_imageThumbSpacing));
						if (numColumns > 0) {
							final int columnWidth = (mGridView.getWidth() / numColumns)
									- m_imageThumbSpacing;
							m_adapter.setItemHeight(columnWidth);

							if (Utils.hasJellyBean()) {
								mGridView.getViewTreeObserver()
										.removeOnGlobalLayoutListener(this);
							} else {
								mGridView.getViewTreeObserver()
										.removeGlobalOnLayoutListener(this);
							}
						}
					}
				});
		return v;

	}

	@Override
	public void onResume() {
		super.onResume();
		m_imageResizer.setExitTasksEarly(false);
		m_adapter.notifyDataSetChanged();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		m_imageResizer.closeCache();
		m_imageResizer.clearCache();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {

		m_imageResizer.setPauseWork(true);

		if(position==0)
		{

			Intent intent=new Intent();
			intent.setClass(getActivity(), RecorderVideoActivity.class);
			startActivityForResult(intent, 100);
		}else{
			VideoEntity vEntty=m_list.get(position-1);
			// 限制大小不能超过10M
			if (vEntty.size > 1024 * 1024 * 10) {
				Toast.makeText(getActivity(), "暂不支持大于10M的视频！", Toast.LENGTH_SHORT).show();
				return;
			}
			Intent intent = getActivity().getIntent().putExtra("path", vEntty.filePath).putExtra("dur", vEntty.duration);
			getActivity().setResult(Activity.RESULT_OK, intent);
			getActivity().finish();
		}
	}

	private class ImageAdapter extends BaseAdapter {

		private final Context mContext;
		private int mItemHeight = 0;
		private RelativeLayout.LayoutParams mImageViewLayoutParams;

		public ImageAdapter(Context context) {

			super();
			mContext = context;
			mImageViewLayoutParams = new RelativeLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		//多一个点击就开始录像的按钮
		@Override
		public int getCount() {
			return m_list.size() + 1;
		}

		@Override
		public Object getItem(int position) {
			return (position == 0) ? null : m_list.get(position - 1);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}


		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			ViewHolder holder = null;
			if (convertView == null) {

				holder = new ViewHolder();

				convertView = LayoutInflater.from(mContext).inflate(R.layout.choose_griditem, container, false);

				holder.imageView = (RecyclingImageView) convertView.findViewById(R.id.imageView);
				holder.icon = (ImageView) convertView.findViewById(R.id.video_icon);
				holder.tvDur = (TextView) convertView.findViewById(R.id.chatting_length_iv);
				holder.tvSize = (TextView) convertView.findViewById(R.id.chatting_size_iv);
				holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				holder.imageView.setLayoutParams(mImageViewLayoutParams);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			// Check the height matches our calculated column width
			if (holder.imageView.getLayoutParams().height != mItemHeight) {
				holder.imageView.setLayoutParams(mImageViewLayoutParams);
			}

			// Finally load the image asynchronously into the ImageView, this
			// also takes care of
			// setting a placeholder image while the background thread runs
			if (position == 0) {
				holder.icon.setVisibility(View.GONE);
				holder.tvDur.setVisibility(View.GONE);
				holder.tvSize.setText("拍摄录像");
				holder.imageView.setImageResource(R.drawable.actionbar_camera_icon);
			} else {
				holder.icon.setVisibility(View.VISIBLE);
				VideoEntity entty = m_list.get(position - 1);
				holder.tvDur.setVisibility(View.VISIBLE);

				//holder.tvDur.setText(DateUtils.toTime(entty.duration));
				//holder.tvSize.setText(TextFormater.getDataSize(entty.size));
				holder.imageView.setImageResource(R.drawable.empty_photo);
				m_imageResizer.loadImage(entty.filePath, holder.imageView);
			}
			return convertView;
			// END_INCLUDE(load_gridview_item)
		}

		/**
		 * Sets the item height. Useful for when we know the column width so the
		 * height can be set to match.
		 *
		 * @param height
		 */
		public void setItemHeight(int height) {
			if (height == mItemHeight) {
				return;
			}
			mItemHeight = height;
			mImageViewLayoutParams = new RelativeLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, mItemHeight);
			m_imageResizer.setImageSize(height);
			notifyDataSetChanged();
		}

		class ViewHolder{
			RecyclingImageView imageView;
			ImageView icon;
			TextView tvDur;
			TextView tvSize;
		}

	}

	private void getVideoFile()
	{
		ContentResolver mContentResolver = getActivity().getContentResolver();

		Cursor cursor 					 = mContentResolver.query(
				MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				null,
				null,
				null,
				MediaStore.Video.DEFAULT_SORT_ORDER
		);
        if (cursor == null) return;
		if (cursor.moveToFirst()) {

			do {
				// ID:MediaStore.Audio.Media._ID
				int id = cursor.getInt(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media._ID));

				// 名称：MediaStore.Audio.Media.TITLE
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
				// 路径：MediaStore.Audio.Media.DATA
				String url = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));

				// 总播放时长：MediaStore.Audio.Media.DURATION
				int duration = cursor
						.getInt(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));

				// 大小：MediaStore.Audio.Media.SIZE
				int size = (int) cursor.getLong(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));

				VideoEntity entty 	= new VideoEntity();

				entty.ID 			= id;
				entty.title 		= title;
				entty.filePath	 	= url;
				entty.duration 		= duration;
				entty.size 			= size;

				m_list.add(entty);
			} while (cursor.moveToNext());

		}

		if (cursor != null) {
			cursor.close();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == 100) {

				String path = data.getStringExtra("uri");

				Intent x = new Intent();
				x.putExtra("path",path);

				getActivity().setResult(Activity.RESULT_OK, x);
				getActivity().finish();
			}
		}
	}
}
