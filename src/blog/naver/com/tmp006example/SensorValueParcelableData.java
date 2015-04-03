package blog.naver.com.tmp006example;

import android.os.Parcel;
import android.os.Parcelable;

public class SensorValueParcelableData implements Parcelable{

	public String UUID;
	public byte[] value;
	public static int v_length;
	
	public SensorValueParcelableData(String uuid, byte[] v) {
		UUID = uuid;
		value = v;
		
		v_length = v.length;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(UUID);
		dest.writeByteArray(value);
		
	}
	
	public static final Parcelable.Creator<SensorValueParcelableData> CREATOR = new Creator<SensorValueParcelableData>() {

		@Override
		public SensorValueParcelableData createFromParcel(Parcel source) {
			
			String got_uuid = source.readString();
			byte[] got_value = new byte[v_length];
			
			source.readByteArray(got_value);
			
			return new SensorValueParcelableData(got_uuid,got_value);
		}

		@Override
		public SensorValueParcelableData[] newArray(int size) {
			
			return new SensorValueParcelableData[size];
		}
		
	};
}
