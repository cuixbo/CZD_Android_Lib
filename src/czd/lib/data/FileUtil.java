package czd.lib.data;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import czd.lib.application.ApplicationUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

/**
 * @author Chen.Zhidong
 *         2011-12-26
 */
public class FileUtil {

	public static final long EXPIRE_DATES = 3;
	public static final int MB = 1024;
	public static final int COPY_MBB = 1;
	public static final int COPY_CHANNEL = 2;
	public static final int COPY_STREAM = 3;
	public static final int COPY_BUFFER = 4;

	/**
	 * return available space in SD card
	 *
	 * @return int (MB)
	 */
	public static int getSDcardSpace() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		double sdfree = ((double)stat.getAvailableBlocks() * (double)stat.getBlockSize()) / MB;
		return (int)sdfree;
	}

	public static boolean sdcardWritable() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && Environment.getExternalStorageDirectory().canWrite())
		{
			return true;
		}
		return false;
	}

	public static long getSize(File path) {
		long size = 0L;
		if (path.exists() && path.isDirectory())
		{
			File[] files = path.listFiles();
			for (File file : files)
			{
				size += getSize(file);
			}
		}
		else if (path.exists() && path.isFile())
		{
			try
			{
				FileInputStream fis = new FileInputStream(path);
				size += fis.available();
				fis.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return size;
	}

	public static void setFileLastModifyTime(File file) {
		if (file.exists() && file.canWrite())
		{
			long newtime = System.currentTimeMillis();
			file.setLastModified(newtime);
		}
	}

	public static void removeExpiredFile(final File filedir) {
		new Thread() {
			@Override
			public void run() {
				super.run();
				File[] files = filedir.listFiles();
				if (files == null)
				{
					return;
				}
				else
				{
					for (File file : files)
					{
						if (file.isDirectory())
						{
							removeExpiredFile(file);
						}
						else if (file.isFile())
						{
							if ((System.currentTimeMillis() - file.lastModified()) / (24 * 60 * 60 * 1000) > EXPIRE_DATES)
							{
								file.delete();
							}
						}
					}
				}
			}
		}.start();
	}

	public static File getCacheDirectory(boolean forceinternal) {
		File cachedir = null;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1 && !forceinternal && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			cachedir = ApplicationUtil.application_context.getExternalCacheDir();
		}
		if (cachedir == null)
		{
			cachedir = ApplicationUtil.application_context.getCacheDir();
		}
		if (cachedir != null && !cachedir.exists() && cachedir.getParentFile().canWrite())
		{
			cachedir.mkdirs();
		}
		return cachedir;
	}

	public static boolean write(File target, byte[] data) {
		if (target.exists())
		{
			target.delete();
		}
		try
		{
			FileOutputStream fos = new FileOutputStream(target);
			fos.write(data);
			fos.flush();
			fos.close();
			return true;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static byte[] read(File source) {
		if (source.exists() && source.canRead())
		{
			try
			{
				FileInputStream fis = new FileInputStream(source);
				byte[] data = new byte[fis.available()];
				fis.read(data);
				fis.close();
				return data;
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String getMIMEType(File file) {
		String type = "";
		String fileName = file.getName();
		String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase(Locale.SIMPLIFIED_CHINESE);
		if (ext.equals("m4a") || ext.equals("mp3") || ext.equals("mid") || ext.equals("xmf") || ext.equals("ogg") || ext.equals("wav"))
		{
			type = "audio";
		}
		else if (ext.equals("3gp") || ext.equals("mp4"))
		{
			type = "video";
		}
		else if (ext.equals("jpg") || ext.equals("gif") || ext.equals("png") || ext.equals("jpeg") || ext.equals("bmp"))
		{
			type = "image";
		}
		else if (ext.equals("apk"))
		{
			/* android.permission.INSTALL_PACKAGES */
			type = "application/vnd.android.package-archive";
		}
		else
		{
			type = "*";
		}
		return type;
	}

	public synchronized static boolean copy(final File source, final File target, final int method) {
		if (source != null && source.canRead())
		{
			if (source.isDirectory())
			{
				File[] files = source.listFiles();
				if (files == null)
				{
					target.mkdirs();
				}
				else
				{
					for (File tmp_file : files)
					{
						if (!copy(tmp_file, new File(target, tmp_file.getName()), method))
							return false;
					}
					return true;
				}
			}
			else
			{
				new Thread() {
					@Override
					public void run() {
						super.run();
						try
						{
							FileInputStream fis = new FileInputStream(source);
							FileOutputStream fos = new FileOutputStream(target);
							switch (method)
							{
								case COPY_MBB:
									FileChannel fci_mbb = fis.getChannel();
									FileChannel fco_mbb = fos.getChannel();
									int length_mbb = 2097152;
									fci_mbb.position(0);
									MappedByteBuffer mbbi_mbb;
									MappedByteBuffer mbbo_mbb;
									while (fci_mbb.position() < fci_mbb.size())
									{
										length_mbb = Math.min(length_mbb, (int)(fci_mbb.size() - fci_mbb.position()));
										mbbi_mbb = fci_mbb.map(FileChannel.MapMode.READ_ONLY, fci_mbb.position(), length_mbb);
										mbbo_mbb = fco_mbb.map(FileChannel.MapMode.READ_WRITE, fco_mbb.size(), length_mbb);
										fci_mbb.position(fci_mbb.position() + length_mbb);
										mbbo_mbb.put(mbbi_mbb);
										mbbi_mbb.clear();
										mbbo_mbb.clear();
									}
									fci_mbb.close();
									fco_mbb.force(true);
									fco_mbb.close();
									break;
								case COPY_CHANNEL:
									FileChannel fci_ch = fis.getChannel();
									FileChannel fco_ch = fos.getChannel();
									int length_ch = 2097152;
									fci_ch.position(0);
									while (fci_ch.position() < fci_ch.size())
									{
										length_ch = Math.min(length_ch, (int)(fci_ch.size() - fci_ch.position()));
										fci_ch.transferTo(fci_ch.position(), length_ch, fco_ch);
										fci_ch.position(fci_ch.position() + length_ch);
									}
									fci_ch.close();
									fco_ch.force(true);
									fco_ch.close();
									break;
								case COPY_BUFFER:
									FileChannel fci_bf = fis.getChannel();
									FileChannel fco_bf = fos.getChannel();
									ByteBuffer bbuffer = ByteBuffer.allocateDirect(4096);
									int b = 0;
									while ((b = fci_bf.read(bbuffer)) != -1)
									{
										bbuffer.flip();
										fco_bf.write(bbuffer);
									}
									fci_bf.close();
									fco_bf.force(true);
									fco_bf.close();
									break;
								case COPY_STREAM:
									byte[] buffer = new byte[4096];
									int s = 0;
									while ((s = fis.read(buffer)) != -1)
									{
										fos.write(buffer);
									}
									break;
							}
							fis.close();
							fos.flush();
							fos.close();
						} catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}.run();
				return true;
			}
		}
		return false;
	}

	public static boolean copy(File source, File target) {

		try
		{
			FileInputStream fis = new FileInputStream(source);
			FileOutputStream fos = new FileOutputStream(target);

			byte[] buffer = new byte[4096];
			int s = 0;
			while ((s = fis.read(buffer)) != -1)
			{
				fos.write(buffer);
			}

			fis.close();
			fos.flush();
			fos.close();
			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static void rm(File file) {
		if (file != null && file.canWrite())
		{
			if (file.isDirectory())
			{
				File[] files = file.listFiles();
				if (files == null)
				{
					file.delete();
				}
				else
				{
					for (File tmp_file : files)
					{
						rm(tmp_file);
					}
				}
			}
			file.delete();
		}
	}

}
