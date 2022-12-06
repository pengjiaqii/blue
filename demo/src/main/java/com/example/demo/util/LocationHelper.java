//package com.example.demo.util;
//
//import android.content.Context;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.net.wifi.WifiInfo;
//import android.net.wifi.WifiManager;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.util.Log;
//import com.baidu.location.BDLocation;
//import com.baidu.location.BDLocationListener;
//import com.baidu.location.LocationClient;
//import com.baidu.location.LocationClientOption;
//import com.baidu.location.LocationClientOption.LocationMode;
//
//
//import java.io.BufferedReader;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.MalformedURLException;
//import java.net.NetworkInterface;
//import java.net.URL;
//import java.net.URLConnection;
//import java.util.Enumeration;
//
//
//public class LocationHelper {
//
//    private static final int BAIDU_API = 1001;
//    private static final int GOOGLE_API = 1000;
//    public static final int STATE_ALL_PROVIDER_DISABLED = 100;
//    public static final int STATE_NETWORK_IO_ERROR = 101;
//    public static final int STATE_OK = 0;
//    private final BDLocationListener mBDLocationListener = new BDLocationListener() {
//        public void onReceiveLocation(BDLocation bdlocation) {
//            StringBuffer stringbuffer;
//            stringbuffer = new StringBuffer(256);
//            stringbuffer.append("time : ");
//            stringbuffer.append(bdlocation.getTime());
//            stringbuffer.append("\nerror code : ");
//            stringbuffer.append(bdlocation.getLocType());
//            stringbuffer.append("\nlatitude : ");
//            stringbuffer.append(bdlocation.getLatitude());
//            stringbuffer.append("\nlontitude : ");
//            stringbuffer.append(bdlocation.getLongitude());
//            stringbuffer.append("\nradius : ");
//            stringbuffer.append(bdlocation.getRadius());
//	    	Log.d("LocationHelper", "1-----location:" + stringbuffer);
//            if(bdlocation.getLocType() == 61) {
//				stringbuffer.append("\nspeed : ");
//				stringbuffer.append(bdlocation.getSpeed());
//				stringbuffer.append("\nsatellite : ");
//				stringbuffer.append(bdlocation.getSatelliteNumber());
//				stringbuffer.append("\ndirection : ");
//				stringbuffer.append("\naddr : ");
//				stringbuffer.append(bdlocation.getAddrStr());
//				stringbuffer.append(bdlocation.getDirection());
//            } else {
//				if(bdlocation.getLocType() == 161) {
//					stringbuffer.append("\naddr : ");
//					stringbuffer.append(bdlocation.getAddrStr());
//					stringbuffer.append("\noperationers : ");
//					stringbuffer.append(bdlocation.getOperators());
//                                        stringbuffer.append("\nLocationDescribe : ");
//                                        stringbuffer.append(bdlocation.getLocationDescribe());
//
//				}
//
//				Log.d("LocationHelper", "location:" + stringbuffer);
//				if((bdlocation.getLocType() == 161
//					|| bdlocation.getLocType() == 61)
//					&& mCb != null) {
//					mCb.onReceived(0, "我的位置：" + bdlocation.getAddrStr() + "http://api.map.baidu.com/marker?location=" + bdlocation.getLatitude() + "," +  bdlocation.getLongitude() + "&title=我的位置&content=" + bdlocation.getAddrStr() + "&output=html&coord_type=gcj02&src=webapp.baidu.openAPIdemo");
//				}
//			}
//        }
//    };
//
//    private ReceivedCallback mCb;
//    private Context mContext;
//    private LocationClient mLocationClient;
//    private LocationManager mLocationManager;
//    private final LocationListener mLocationListener = new LocationListener() {
//
//        public void onLocationChanged(final Location location) {
//            new AsyncTask() {
//                protected Object doInBackground(Object aobj[]) {
//                    return doInBackground((Void[])aobj);
//                }
//
//                protected String doInBackground(Void avoid[]) {
//                    return getAccuracyAddrByNetServer(location);
//                }
//
//                protected void onPostExecute(Object obj) {
//                    onPostExecute((String)obj);
//                }
//
//                protected void onPostExecute(String s) {
//                    stopGetLocation();
//                    if(mCb == null);
//                }
//            }.execute();
//        }
//
//        public void onProviderDisabled(String s) {
//			//
//        }
//
//        public void onProviderEnabled(String s) {
//			//
//        }
//
//        public void onStatusChanged(String s, int i, Bundle bundle) {
//			//
//        }
//    };
//
//
//    public static interface ReceivedCallback {
//        public abstract void onReceived(int i, String s);
//    }
//
//
//    public LocationHelper(Context context) {
//        mContext = context;
//        Log.d("LocationHelper", "LocationHelper:");
//        mLocationClient = ((SOSApplication)mContext.getApplicationContext()).mLocationClient;
//        mLocationClient.registerLocationListener(mBDLocationListener);
//        LocationClientOption lco = new LocationClientOption();
//        lco.setLocationMode(LocationMode.Battery_Saving);
//        lco.setCoorType("gcj02");
//        lco.setScanSpan(5000);
//        lco.setIsNeedAddress(true);
//        mLocationClient.setLocOption(lco);
//    }
//
//    private String getAccuracyAddrByNetServer(Location location) {
//        double d = location.getLatitude();
//        double d1 = location.getLongitude();
//        String s = "http://maps.google.com/maps/api/geocode/json?latlng=" + d + "," + d1 + "&language=zh-CN&sensor=false";
//        String s1 = "http://api.map.baidu.com/geocoder/v2/?ak=NnpUtFD10jF3pwjwwuqU7C1H&location=" + d + "," + d1 + "&output=json&pois=0";
//
//		/*try {
//			HttpURLConnection hurl = (HttpURLConnection)(new URL(s)).openConnection();
//	        hurl.setConnectTimeout(5000);
//	        hurl.setRequestMethod("GET");
//	        hurl.setDoInput(true);
//	        if(hurl.getResponseCode() != 200)
//	            return null;
//
//	        String s3 = parseAddr(hurl.getInputStream(), 1000);
//	        return s3;
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			return null;
//		}*/
//
//		try {
//	        HttpURLConnection hurl1 = (HttpURLConnection)(new URL(s1)).openConnection();
//	        hurl1.setConnectTimeout(5000);
//	        hurl1.setRequestMethod("GET");
//	        hurl1.setDoInput(true);
//	        if(hurl1.getResponseCode() != 200)
//	            return null;
//	        String s2 = parseAddr(hurl1.getInputStream(), 1001);
//	        return s2;
//		} catch (Exception ex1) {
//			ex1.printStackTrace();
//		}
//        return null;
//    }
//
//    private String getIp() {
//        WifiManager wifimanager = (WifiManager)mContext.getSystemService("wifi");
//        if(wifimanager.isWifiEnabled()) {
//            WifiInfo wifiinfo = wifimanager.getConnectionInfo();
//            if(wifiinfo != null) {
//                return intToIp(wifiinfo.getIpAddress());
//            }
//        }
//
//		try {
//	        Enumeration enumeration = NetworkInterface.getNetworkInterfaces();
//
//			while(enumeration.hasMoreElements()) {
//				Enumeration enumeration1 = ((NetworkInterface)enumeration.nextElement()).getInetAddresses();
//				while(enumeration1.hasMoreElements()) {
//					InetAddress inetaddress = (InetAddress)enumeration1.nextElement();
//					if(!inetaddress.isLoopbackAddress()
//						&& (inetaddress instanceof Inet4Address)) {
//						String s = inetaddress.getHostAddress().toString();
//						return s;
//					}
//				}
//			}
//		} catch(Exception ex) {
//			ex.printStackTrace();
//			Log.e("WifiPreference IpAddress", ex.toString());
//		}
//        return null;
//    }
//
//    private Location getLocationByIp(String s) {
//        if(s != null && !"".equals(s)) {
//			String s1 = "http://api.map.baidu.com/location/ip?ak=NnpUtFD10jF3pwjwwuqU7C1H&location=&ip=" + s + "&coor=bd09ll";
//			try {
//				HttpURLConnection hrlc = (HttpURLConnection)(new URL(s1)).openConnection();
//				hrlc.setConnectTimeout(5000);
//				hrlc.setRequestMethod("GET");
//				hrlc.setDoInput(true);
//				if(hrlc.getResponseCode() != 200) {
//					return null;
//				} else {
//					Location location = parseLocation(hrlc.getInputStream());
//					return location;
//				}
//			} catch(Exception ex) {
//				ex.printStackTrace();
//			}
//		}
//        return null;
//    }
//
//    public static String getNetIp() {
//		InputStream inputstream = null;
//		BufferedReader bufferedreader;
//		StringBuilder stringbuilder;
//
//		try {
//			URL url = new URL("http://iframe.ip138.com/ic.asp");
//			HttpURLConnection httpurlconnection = (HttpURLConnection)url.openConnection();
//
//	        if(httpurlconnection.getResponseCode() != 200) {
//				return null;
//			} else {
//				inputstream = httpurlconnection.getInputStream();
//				bufferedreader = new BufferedReader(new InputStreamReader(inputstream, "utf-8"));
//				stringbuilder = new StringBuilder();
//			}
//
//			String s = bufferedreader.readLine();
//			while(s != null) {
//				stringbuilder.append(s + "\n");
//				s = bufferedreader.readLine();
//			}
//
//	        int i = stringbuilder.indexOf("[");
//	        int j = stringbuilder.indexOf("]", i + 1);
//	        String s1 = stringbuilder.substring(i + 1, j);
//	        return s1;
//		} catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (inputstream != null)
//                    inputstream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//		return null;
//    }
//
//    private String intToIp(int i) {
//        return (new StringBuilder()).append(i & 0xff).append(".").append(0xff & i >> 8).append(".").append(0xff & i >> 16).append(".").append(0xff & i >> 24).toString();
//    }
//
//    private String parseAddr(InputStream inputstream, int i) {
//        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
//        byte abyte0[] = new byte[1024];
//        String s = null;
//
//		try {
//	        int l = inputstream.read(abyte0);
//	        while(-1 != l) {
//				bytearrayoutputstream.write(abyte0, 0, l);
//				l = inputstream.read(abyte0);
//			}
//			s = new String(bytearrayoutputstream.toByteArray());
//
//	        if(s != null && s.indexOf("formatted_address") > 0) {
//	            int j;
//	            int k;
//	            String s1;
//	            if(i == 1001) {
//	                j = 3 + (s.indexOf("formatted_address") + "formatted_address".length());
//	                k = -1 + s.indexOf(',', j);
//	            } else {
//	                j = 5 + (s.indexOf("formatted_address") + "formatted_address".length());
//	                k = s.indexOf(' ', j);
//	            }
//	            return s.substring(j, k);
//	        } else {
//	            return null;
//	        }
//		} catch(Exception ex) {
//			ex.printStackTrace();
//		}
//		return s;
//    }
//
//    private Location parseLocation(InputStream inputstream) {
//        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
//        byte abyte0[] = new byte[1024];
//        String s = null;
//
//		try {
//	        int k = inputstream.read(abyte0);
//	        while(-1 != k) {
//				bytearrayoutputstream.write(abyte0, 0, k);
//				k = inputstream.read(abyte0);
//			}
//			s = new String(bytearrayoutputstream.toByteArray());
//	        if(s != null && s.indexOf("point") > 0) {
//	            int i = 5 + s.indexOf("\"x\":");
//	            double d = Double.valueOf(s.substring(i, s.indexOf("\",", i))).doubleValue();
//	            int j = 5 + s.indexOf("\"y\":");
//	            double d1 = Double.valueOf(s.substring(j, s.indexOf("\"}", j))).doubleValue();
//	            Location location = new Location("network");
//	            location.setLatitude(d1);
//	            location.setLongitude(d);
//	            return location;
//	        } else {
//	            return null;
//	        }
//		} catch(Exception ex) {
//			ex.printStackTrace();
//		}
//
//		return null;
//    }
//
//    public void setReceivedCallback(ReceivedCallback receivedcallback) {
//        mCb = receivedcallback;
//    }
//
//    public void startGetLocation() {
//        mLocationClient.start();
//        if(mLocationManager != null);
//    }
//
//    public void stopGetLocation() {
//        mLocationClient.stop();
//    }
//
//}
