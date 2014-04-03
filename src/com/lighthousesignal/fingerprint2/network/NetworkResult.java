package com.lighthousesignal.fingerprint2.network;

public class NetworkResult {
	private int mResponseCode;
	private Exception mException;
	private NetworkTask mTask;
	private byte[] mData;
	public int getResponseCode() {
		return mResponseCode;
	}
	public void setResponseCode(int responceCode) {
		mResponseCode = responceCode;
	}
	public Exception getException() {
		return mException;
	}
	public void setException(Exception exception) {
		mException = exception;
	}
	public NetworkTask getTask() {
		return mTask;
	}
	
	public void setTask(NetworkTask task) {
		mTask = task;
	}
	
	public byte[] getData() {
		return mData;
	}
	
	public String getDataString() {
		return new String(mData);
	}
	
	public void setData(byte[] data) {
		mData = data;
	}

	@Override
	public String toString() {
		return "RespCode=" + mResponseCode + "\nExc=" + mException + "\n" + " data:" + (mData == null? null : new String(mData));
	}
}
