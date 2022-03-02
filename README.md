# java geotools 处理gis类相关问题

#### 介绍
使用GeoTools 目前有的功能：
1. 解析shp 点线面文件 获取数据详情
2. shp数据转成GeoJSON
3. shp文件转esrijson，逻辑上是先将shp转成GeoJSON文件，再将GeoJSON转成esrijson
4. 主要功能是将GeoJSON生成图片，同理shp也可以先转GeoJSON再生成图片。这个功能主要解决了geotools工具在处理数据时properties属性不一致问题（不过多描述）

### 说明
1. 功能会随着我的业务我会不断添加进来，如果大家有run不起来的，留言我解决，我是从业务代码中提取出来的这些功能，有可能有错误的地方。
1. 我也借鉴了网上的一些案例，有些可能就是解决了一些兼容问题或者一些bug，记录在git上一方面自己做个留档，一方面也是希望能够帮助有需要的人