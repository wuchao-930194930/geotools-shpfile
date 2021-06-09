package com.xiaowuzian.tools;

import com.alibaba.fastjson.JSONObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.json.simple.JSONArray;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.*;
/**
 * @Author 小伍子安
 * @Date 2021/3/16 上午11:33
 * @Version 1.0
 */
public class ParsingShpFileUtils {

    public static void main(String[] args) throws Exception {
//        System.out.println(ParsingShpFile("/Users/wuchao/Documents/bengzhan.shp"));
        String geojson = shape2Geojson("/Users/wuchao/Documents/项目/厦门水务/gis/trans-3/bengzhan.shp", "/Users/wuchao/Desktop/shptogeojson.json");
        System.out.println(geojson);
    }

    /**
     * 解析shp文件
     * @param filePath
     * @return
     * @throws Exception
     */
    public static Map ParsingShpFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()){
            throw new Exception("文件不存在!");
        }
        if (!filePath.endsWith("shp")){
            throw new Exception("只能指定后缀为shp的文件");
        }
        Map map = new HashMap();
        List<Map> list = new ArrayList();
        //读取shp
        SimpleFeatureCollection colls1 = readShp(filePath);
        SimpleFeatureType schema = colls1.getSchema();
        Name name = schema.getGeometryDescriptor().getType().getName();
        ReferencedEnvelope bounds = colls1.getBounds();
        //拿到所有features
        SimpleFeatureIterator iters = colls1.features();
        String s = name.toString();
        if ("Point".equals(s)) {
            list = parsingPoint(iters);
        } else if ("MultiLineString".equals(s) || "MultiPolygon".equals(s)) {
            list = parsingLineOrPoly(iters);
        }
        map.put("data",list);
        map.put("maxX",bounds.getMaxX());
        map.put("minX",bounds.getMinX());
        map.put("maxY",bounds.getMaxY());
        map.put("minY",bounds.getMinY());
        map.put("shapeFile",name.toString());
        return map;
    }
    /**
     * 解析点数据
     *
     * @param iters
     * @return
     */
    public static List<Map> parsingPoint(SimpleFeatureIterator iters) {
        List<Map> list = new ArrayList();
        while (iters.hasNext()) {
            SimpleFeature sf = iters.next();
            Map map = new HashMap();
            Iterator<? extends Property> iterator = sf.getValue().iterator();
            while (iterator.hasNext()) {
                Property property = iterator.next();
                if (property.getValue() instanceof Point) {
                    map.put("PointX", ((Point)(property.getValue())).getX());
                    map.put("PointY", ((Point)(property.getValue())).getY());
                }else{
                    Name name = property.getName();//属性名称
                    Object value = property.getValue();//属性值
                    map.put(name,value);
                }
            }
            list.add(map);
        }
        return list;
    }

    /**
     * 解析线和面
     *
     * @param iters
     * @return
     */
    public static List<Map> parsingLineOrPoly(SimpleFeatureIterator iters) {
        List<Map> list = new ArrayList();
        while (iters.hasNext()) {
            SimpleFeature sf = iters.next();
            Map map = new HashMap();
            Iterator<? extends Property> iterator = sf.getValue().iterator();
            while (iterator.hasNext()) {
                Property property = iterator.next();
                if (property.getValue() instanceof Geometry) {
                    Geometry geometry = (Geometry) property.getValue();
                    Coordinate[] coordinates = geometry.getCoordinates();
                    List<Map> paths = new ArrayList<Map>();
                    for (Coordinate coordinate : coordinates) {
                        Map path = new HashMap();
                        path.put("x",coordinate.x);
                        path.put("y",coordinate.y);
                        path.put("z",coordinate.z);
                        paths.add(path);
                    }
                    map.put("path",paths);
                }else{
                    Name name = property.getName();//属性名称
                    Object value = property.getValue();//属性值
                    map.put(name,value);
                }
            }
            list.add(map);
        }
        return list;
    }
    public static SimpleFeatureCollection readShp(String path) {
        return readShp(path, null);

    }

    public static SimpleFeatureCollection readShp(String path, Filter filter) {
        SimpleFeatureSource featureSource = readStoreByShp(path);
        if (featureSource == null) {return null;};
        try {
            return filter != null ? featureSource.getFeatures(filter) : featureSource.getFeatures();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SimpleFeatureSource readStoreByShp(String path) {
        File file = new File(path);
        FileDataStore store;
        SimpleFeatureSource featureSource = null;
        try {
            store = FileDataStoreFinder.getDataStore(file);
            ((ShapefileDataStore) store).setCharset(Charset.forName("UTF-8"));
            featureSource = store.getFeatureSource();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return featureSource;
    }
    /**
     * shp转换为Geojson
     * @param shpPath shp文件地址
     * @param jsonPath 要写入的json文件地址
     * @return
     */
    public static String shape2Geojson(String shpPath, String jsonPath){
        FeatureJSON fjson = new FeatureJSON();
        StringBuffer sb = new StringBuffer();
        try{
            sb.append("{\"type\": \"FeatureCollection\",\"features\": ");

            //读取shp
            SimpleFeatureCollection colls = readShp(shpPath);
            //拿到所有features
            SimpleFeatureIterator itertor = colls.features();
            JSONArray array = new JSONArray();
            while (itertor.hasNext())
            {
                SimpleFeature feature = itertor.next();
                StringWriter writer = new StringWriter();
                fjson.writeFeature(feature, writer);
                JSONObject json = JSONObject.parseObject(writer.toString());
                array.add(json);
            }
            itertor.close();
            sb.append(array.toString());
            sb.append("}");

            //写入文件
            FileOutputStream fos = new FileOutputStream(jsonPath,false);
//true表示在文件末尾追加
            fos.write(sb.toString().getBytes());
            fos.close();
        }
        catch(Exception e){
            e.printStackTrace();

        }
        return sb.toString();
    }
}
