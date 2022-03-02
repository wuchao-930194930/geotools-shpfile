package com.xiaowuzian.tools;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.tomcat.util.codec.binary.Base64;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.label.LabelCacheImpl;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * @Author 小伍子安
 * @Date 2021/12/2 下午5:17
 * @Version 1.0
 */
public class GeojsonToImageUtils {
    /**
     * 获取GeoJSON图片
     * @param json
     * @param imagePath
     * @return
     * @throws IOException
     */
    public static String getGeojsonBase64(String json,String imagePath) throws IOException {
        String imgStr="iVBORw0KGgoAAAANSUhEUgAAAYAAAAGACAYAAACkx7W/AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyVpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNi1jMTQ4IDc5LjE2NDAzNiwgMjAxOS8wOC8xMy0wMTowNjo1NyAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIDIxLjAgKE1hY2ludG9zaCkiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6NUYyMzUzNzk0QzJFMTFFQzhFMjZGNkU0RTBFNENDMDQiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6NUYyMzUzN0E0QzJFMTFFQzhFMjZGNkU0RTBFNENDMDQiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW5zdGFuY2VJRD0ieG1wLmlpZDo1RjIzNTM3NzRDMkUxMUVDOEUyNkY2RTRFMEU0Q0MwNCIgc3RSZWY6ZG9jdW1lbnRJRD0ieG1wLmRpZDo1RjIzNTM3ODRDMkUxMUVDOEUyNkY2RTRFMEU0Q0MwNCIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/Pk7/KegAABXKSURBVHja7N37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";//默认图片
        try {
            String s = removeGeoJsonProperties(json);
            String s1 = geojson2img(s, imagePath);
            imgStr = getImgStr(s1);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "data:image/png;base64,"+imgStr;
    }
    /**
     * 生成图片
     * @param json
     * @param imgPath
     * @throws IOException
     */
    public static String geojson2img(String json,String imgPath) throws IOException {
        String filename = UUID.randomUUID().toString()+".png";
        FeatureJSON featureJSON = new FeatureJSON();
        FeatureCollection features = featureJSON.readFeatureCollection(json);
        MapContent mapContent = new MapContent();
        mapContent.setTitle("Quickstart");
        Style style = getStyle(features,json);
        Layer layer = new FeatureLayer(features, style);
        mapContent.addLayer(layer);
        File outputFile = new File(imgPath+filename);
        ImageOutputStream outputImageFile = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(outputFile);
            outputImageFile = ImageIO.createImageOutputStream(fileOutputStream);
            int w = 384;
            ReferencedEnvelope bounds = features.getBounds();
            int h = (int) (w * (768 / 506));
            BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            mapContent.getViewport().setMatchingAspectRatio(true);

            mapContent.getViewport().setScreenArea(new Rectangle(Math.round(w), Math.round(h)));
            mapContent.getViewport().setBounds(bounds);

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle outputArea = new Rectangle(250, 250);
            outputArea.setLocation(70,100);
            GTRenderer renderer = new StreamingRenderer();
            LabelCacheImpl labelCache = new LabelCacheImpl();
            Map<Object, Object> hints = renderer.getRendererHints();
            if (hints == null) {
                hints = new HashMap<Object, Object>();
            }
            hints.put(StreamingRenderer.LABEL_CACHE_KEY, labelCache);
            renderer.setRendererHints(hints);
            renderer.setMapContent(mapContent);
            renderer.paint(g2d, outputArea, bounds);
            ImageIO.write(bufferedImage, "png", outputImageFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (outputImageFile != null) {
                    outputImageFile.flush();
                    outputImageFile.close();
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            } catch (IOException e) {// don't care now
            }
        }
        return imgPath+filename;
    }

    /**
     * 生成样式
     * @param features
     * @param json
     * @return
     */
    public static Style getStyle(FeatureCollection features, String json){
        Integer geojsonType = getGeojsonType(json);
        if (geojsonType == 1){
            Style point = SLD.createSimpleStyle(features.getSchema(),roundColor());
            return point;
        }else if (geojsonType ==2){
            Style lineStyle = SLD.createLineStyle(roundColor(), 1);
            return lineStyle;
        }else if(geojsonType == 3){
            Style polygonStyle = SLD.createPolygonStyle(Color.white, roundColor(), 1);
            return polygonStyle;
        }
        return null;
    }

    /**
     * 随机颜色
     * @return
     */
    public static Color roundColor(){
        Random random = new Random();
        float hue = random.nextFloat();
        float saturation = (random.nextInt(2000) + 1000) / 10000f;
        float luminance = 0.9f;
        Color color = Color.getHSBColor(hue, saturation, luminance);
        return color;
    }

    /**
     * 去除properties属性
     * @param jsonstr
     * @return
     */
    public static String removeGeoJsonProperties(String jsonstr){
        JSONObject json = (JSONObject) JSONObject.parse(jsonstr);
        JSONArray features = (JSONArray) json.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            feature.remove("properties");
        }
        return json.toJSONString();
    }
    /**
     * 获取GeoJSON类型
     * @param strJson
     * @return
     */
    public static Integer getGeojsonType(String strJson){
        JSONObject json = (JSONObject) JSONObject.parse(strJson);
        JSONArray features = (JSONArray) json.get("features");
        JSONObject feature0 = features.getJSONObject(0);
        String strType = ((JSONObject)feature0.get("geometry")).getString("type").toString();
        Integer geoType = null;
        if ("Point".equals(strType)) {
            geoType = 1;
        } else if ("MultiPoint".equals(strType)) {
            geoType = 1;
        } else if ("LineString".equals(strType)) {
            geoType = 2;
        } else if ("MultiLineString".equals(strType)) {
            geoType = 2;
        } else if ("Polygon".equals(strType)) {
            geoType = 3;
        } else if ("MultiPolygon".equals(strType)) {
            geoType = 3;
        }
        return geoType;
    }
    /**
     * 将图片转换成Base64编码
     * @param imgFile 待处理图片
     * @return
     */
    public static String getImgStr(String imgFile) {
        // 将图片文件转化为字节数组字符串，并对其进行Base64编码处理

        InputStream in = null;
        byte[] data = null;
        // 读取图片字节数组
        try {
            in = new FileInputStream(imgFile);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeBase64String(data);
    }

    /**
     * 判断GeoJSON格式是否正确
     * @param strJson
     * @return
     */
    public static boolean checkGeojson(String strJson){
        Boolean flag = true;
        JSONObject json = (JSONObject) JSONObject.parse(strJson);
        if(!json.containsKey("features")){
            return false;
        }
        JSONArray features = (JSONArray) json.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            if (!feature.containsKey("geometry")){
                flag = false;
                break;
            }
            JSONObject geometry = (JSONObject)feature.get("geometry");
            if (!geometry.containsKey("type")){
                flag = false;
                break;
            }
            if (!geometry.containsKey("coordinates")){
                flag = false;
                break;
            }

        }
        return flag;
    }
    public static void main(String[] args) throws IOException {
        String shpPath = "";//shp文件地址
        String geojsonPath = "";//生成的GeoJSON文件地址
        String iamgepath="";
        String geojson = ParsingShpFileUtils.shape2Geojson(shpPath, geojsonPath);
        String geojsonBase64 = getGeojsonBase64(geojson, iamgepath);//base64 一般存库里
    }
}


