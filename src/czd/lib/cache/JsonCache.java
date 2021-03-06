package czd.lib.cache;

import czd.lib.application.ApplicationUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import czd.lib.data.FileUtil;
import czd.lib.data.PreferenceUtil;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: chenzhidong
 * Date: 13-12-30
 * Time: 上午11:56
 */
public class JsonCache extends FileCache {
	protected static JsonCache instance;

	public JsonCache() {
		super();
		this.name = "json";
	}

	public static JsonCache getInstance() {
		if (instance == null)
			instance = new JsonCache();
		return instance;
	}

	@Override
	public boolean save(final String key, final Object value) {
		cleanOld();
		final File file = genFile(key);
		if (file.exists() && file.isFile())
			file.delete();
		if (value instanceof JSONObject)
		{
			PreferenceUtil.writeLongPreference(ApplicationUtil.application_context, this.name, genKey(key), System.currentTimeMillis());
			writer.execute(new Runnable() {
				@Override
				public void run() {
					FileUtil.write(file, ((JSONObject)value).toString().getBytes());
				}
			});
		}

		else if (value instanceof JSONArray)
		{
			PreferenceUtil.writeLongPreference(ApplicationUtil.application_context, this.name, genKey(key), System.currentTimeMillis());
			writer.execute(new Runnable() {
				@Override
				public void run() {
					FileUtil.write(file, ((JSONArray)value).toString().getBytes());
				}
			});
		}
		else
			super.save(key, value);

		return true;
	}

	@Override
	public Object get(String key) {
		File file = genFile(key);
		String data = new String(FileUtil.read(file));
		if (data.startsWith("{") || data.startsWith("["))
		{
			try
			{
				return new JSONTokener(data).nextValue();
			} catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}
}
