package com.xiaowuzian.tools;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * 小伍子安
 * 将shp文件转为esrijson文件  逻辑是先将shp转为GeoJSON  然后再将GeoJSON转为esrijson
 */
public class EsriGeoJsonJsUtils {
    public static void main(String[] args) throws IOException {
        String shpPath = "";//shp文件地址
        String geojsonPath = "";//生成的GeoJSON文件地址
        String esrijsonpath="";
        String geojson = ParsingShpFileUtils.shape2Geojson(shpPath, geojsonPath);
        String esrijson = geo2ersi(geojson, "");
        //写入文件
        FileOutputStream fos = new FileOutputStream(esrijsonpath,false);
//true表示在文件末尾追加
        fos.write(esrijson.toString().getBytes());
        fos.close();
        System.out.println(esrijson);
    }
    //EsriJson转geoJson
    public static String esri2geo(String ersiJson){
        Map geoMap = new HashMap();
        try {
            List geoFs = new ArrayList();
            geoMap.put("type", "FeatureCollection");
            Map esriMap = (Map) JSON.parse(ersiJson);
            Object esriFs = esriMap.get("features");
            if(esriFs instanceof List){
                esriFs = (List<Map<String, Object>>) esriFs;
                for(int i=0; i< ((List) esriFs).size(); i++){
                    Map esriF = (Map) ((List) esriFs).get(i);
                    Map geoF = new HashMap();
                    geoF.put("type", "Feature");
                    geoF.put("properties", esriF.get("attributes"));
                    Map<String, Object> geometry = (Map<String, Object>) esriF.get("geometry");
                    if(null != geometry.get("x")){
                        geoF.put("geometry", geoPoint(geometry));
                    }else if(null != geometry.get("points")){
                        geoF.put("geometry", geoPoints(geometry));
                    }else if(null != geometry.get("paths")){
                        geoF.put("geometry", geoLine(geometry));
                    }else if(null != geometry.get("rings")){
                        geoF.put("geometry", geoPoly(geometry));
                    }
                    geoFs.add(geoF);
                }
                geoMap.put("features", geoFs);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new JSONObject(geoMap).toString();
    }
    //geoJosn转EsriJson
    public static String geo2ersi(String geoJson, String idAttribute){
        Map esriMap = new HashMap();
        try {
//            System.out.println(geoJson);
//            Map geoMap = (Map) JSON.parse(geoJson);
            Map geoMap = JSON.parseObject(geoJson);
            esriMap = getEsriGeo(geoMap, idAttribute);
            Map spatialReference = new HashMap();
            spatialReference.put("wkid", 4326);
            esriMap.put("spatialReference",spatialReference);
        }catch (Exception e){
            e.printStackTrace();
        }
        return new JSONObject(esriMap).toString();
    }

    public static Map getEsriGeo(Map geoMap, String idAttribute){
        Map esriMap = new HashMap();
        if (idAttribute == null || "".equals(idAttribute)){
            idAttribute = "OBJECTID";
        }
        String type = geoMap.get("type").toString();
        if ("Point".equals(type)) {
            List<BigDecimal> coords = (List<BigDecimal>) geoMap.get("coordinates");
            esriMap.put("x", coords.get(0));
            esriMap.put("y", coords.get(1));
        } else if ("MultiPoint".equals(type)) {
            esriMap.put("points", geoMap.get("coordinates"));
        } else if ("LineString".equals(type)) {
            List<Object> coordsList = new ArrayList<Object>();
            coordsList.add(geoMap.get("coordinates"));
            esriMap.put("paths", coordsList);
        } else if ("MultiLineString".equals(type)) {
            esriMap.put("paths", geoMap.get("coordinates"));
        } else if ("Polygon".equals(type)) {
            List<List<List<BigDecimal>>> coordinates = (List<List<List<BigDecimal>>>) geoMap.get("coordinates");
            List<List<List<BigDecimal>>> rings = orientRings(coordinates);
            esriMap.put("rings", rings);
        } else if ("MultiPolygon".equals(type)) {
            List<List<List<List<BigDecimal>>>> mcoordinates = (List<List<List<List<BigDecimal>>>>) geoMap.get("coordinates");
            List<List<List<BigDecimal>>> mrings = flattenMultiPolygonRings(mcoordinates);
            esriMap.put("rings", mrings);
        } else if ("Feature".equals(type)) {
            if (null != geoMap.get("geometry")) {
                Map geometry = getEsriGeo((Map) geoMap.get("geometry"), idAttribute);
                esriMap.put("geometry", geometry);
            }
            if (null != geoMap.get("properties")) {
                Map properties = (Map) geoMap.get("properties");
                if (null != geoMap.get("id")) {
                    properties.put(idAttribute, geoMap.get("id"));
                }
                esriMap.put("attributes", properties);
            }
        } else if ("FeatureCollection".equals(type)) {
            List<Object> esriFs = new ArrayList<Object>();
            List<Map> features = (List<Map>) geoMap.get("features");
            for (int i = 0; i < features.size(); i++) {
                esriFs.add(getEsriGeo(features.get(i), idAttribute));
            }
            esriMap.put("features", esriFs);
            esriMap.put("geometryType", "esriGeometryPolygon");
        } else if ("GeometryCollection".equals(type)) {
            List<Object> esriFsc = new ArrayList<Object>();
            List<Map> geometries = (List<Map>) geoMap.get("geometries");
            for (int i = 0; i < geometries.size(); i++) {
                esriFsc.add(getEsriGeo(geometries.get(i), idAttribute));
            }
            esriMap.put("geometries", esriFsc);
            esriMap.put("geometryType", "esriGeometryPolygon");
        }
        return esriMap;
    }


    public static Map geoPoint(Map<String, Object> geometry){
        Map geo = new HashMap();
        geo.put("type", "point");
        BigDecimal x = (BigDecimal) geometry.get("x");
        BigDecimal y = (BigDecimal) geometry.get("y");
        List<BigDecimal> coords = new ArrayList<BigDecimal>();
        coords.add(x);
        coords.add(y);
        geo.put("coordinates", coords);
        return geo;
    }

    public static Map geoPoints(Map<String, Object> geometry){
        Map geo = new HashMap();
        List<Object> points = (List<Object>) geometry.get("points");
        if(points.size()==1){
            geo.put("type", "Point");
            geo.put("coordinates", points.get(0));
        }else{
            geo.put("type", "MultiPoint");
            geo.put("coordinates", points);
        }
        return geo;
    }

    public static Map geoLine(Map<String, Object> geometry){
        Map geo = new HashMap();
        List<Object> paths = (List<Object>) geometry.get("paths");
        if(paths.size()==1){
            geo.put("type", "LineString");
            geo.put("coordinates", paths.get(0));
        }else{
            geo.put("type", "MultiLineString");
            geo.put("coordinates", paths);
        }
        return geo;
    }

    public static Map geoPoly(Map<String, Object> geometry){
        Map geo = new HashMap();
        List<List<List<BigDecimal>>> rings = (List<List<List<BigDecimal>>>) geometry.get("rings");
        if(rings.size()==1){
            geo.put("type", "Polygon");
            geo.put("coordinates", rings);
        }else{
            List<List<List<List<BigDecimal>>>> coords = new ArrayList();
            String type = "";
            int len = coords.size() - 1;
            for(int i=0; i< rings.size(); i++){
                if(ringIsClockwise( rings.get(i))){
                    List<List<List<BigDecimal>>> item = new ArrayList<List<List<BigDecimal>>>();
                    item.add(rings.get(i));
                    coords.add(item);
                    len++;
                }else{
                    coords.get(len).add(rings.get(i));
                }
            }
            if(coords.size() == 1){
                type="Polygon";
            }else{
                type="MultiPolygon";
            }
            geo.put("type", type);
            geo.put("coordinates",coords.size()==1?coords.get(0): coords);
        }
        return geo;
    }

    public static boolean ringIsClockwise(List<List<BigDecimal>> rings){
        int total = 0;
        List<BigDecimal> pt1 = null;
        List<BigDecimal> pt2 = null;
        for(int i=0; i< rings.size()-1; i++){
            pt1 = rings.get(i);
            pt2 = rings.get(i+1);
               Object pt20 =  pt2.get(0);
                Object pt10 = pt1.get(0);
                Object pt21 = pt2.get(1);
                Object pt11 = pt1.get(1);
                if (pt21 instanceof Integer) {
                    pt21 = new BigDecimal(pt21.toString());
                }
                if (pt20 instanceof Integer) {
                    pt20 = new BigDecimal(pt20.toString());
                }
                if (pt10 instanceof Integer) {
                    pt10 = new BigDecimal(pt10.toString());
                }
                if (pt11 instanceof Integer) {
                    pt11 = new BigDecimal(pt11.toString());
                }
            total += (((BigDecimal)pt20).doubleValue() - ((BigDecimal)pt10).doubleValue())*  (((BigDecimal)pt21).doubleValue() + ((BigDecimal)pt11).doubleValue());
        }
        return total>=0;
    }

    public static List<List<List<BigDecimal>>> orientRings ( List<List<List<BigDecimal>>> polygon) {
        List<List<List<BigDecimal>>> ringsList = new ArrayList<List<List<BigDecimal>>>();
        List<List<BigDecimal>> outerRing = closeRing(polygon.get(0));
        if (outerRing.size() >= 4) {
            if (!ringIsClockwise(outerRing)) {
                Collections.reverse(outerRing);
            }
            ringsList.add(outerRing);
            polygon.remove(0);
            for (int i = 0; i < polygon.size(); i++) {
                List<List<BigDecimal>> hole = closeRing(polygon.get(i));
                if (hole.size() >= 4) {
                    if (ringIsClockwise(hole)) {
                        Collections.reverse(hole);
                    }
                    ringsList.add(hole);
                }
            }
        }
        return ringsList;
    }

    public static List<List<BigDecimal>> closeRing (List<List<BigDecimal>> coords) {
        if (!pointsEqual(coords.get(0), coords.get(coords.size()-1))) {
            coords.add(coords.get(0));
        }
        return coords;
    }

    public static boolean pointsEqual (List<BigDecimal> a, List<BigDecimal> b) {
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).compareTo(b.get(i)) != 0) {
                return false;
            }
        }
        return true;
    }

    public static List<List<List<BigDecimal>>> flattenMultiPolygonRings (List<List<List<List<BigDecimal>>>> rings) {
        List<List<List<BigDecimal>>> polygonList = new ArrayList<List<List<BigDecimal>>>();
        for (int i = 0; i < rings.size(); i++) {
            List<List<List<BigDecimal>>> polygons = orientRings(rings.get(i));
            for (int x = polygons.size() - 1; x >= 0; x--) {
                List<List<BigDecimal>> polygon = polygons.get(x);
                polygonList.add(polygon);
            }
        }
        return polygonList;
    }

}
