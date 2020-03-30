
### spring boot

安装 jdk、maven, `java -version` 和 `mvn -v` 有对应的值即可.  
安装 idea 并添加 lombok 和 mybatis 插件(如果使用 eclipse 只安装 lombok 插件就可以了).  
基于 [hosts 文件](document/hosts.md) 修改本机 hosts 文件.

将下面的内容保存成 `settings.xml` 并放到 `${user.home}/.m2`, 想要改变仓库地址, 注释解开并设置即可.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <!-- <localRepository>D:/repository</localRepository> -->

    <mirrors>
        <mirror>
            <id>aliyun</id>
            <name>aliyun maven</name>
            <mirrorOf>central</mirrorOf>
            <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
        </mirror>
    </mirrors>

    <profiles>
        <profile>
            <id>company</id>
            <repositories>
                <repository>
                    <id>aliyun-repository</id>
                    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </repository>
            </repositories>
            
            <pluginRepositories>
                <pluginRepository>
                    <id>aliyun-plugin</id>
                    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>company</activeProfile>
    </activeProfiles>
</settings>
```
基于数据库生成 mybatis 文件请查看 [源码生成](https://github.com/liuanxin/mybatis-gen)


### 分页示例

在 controller 中这样
```java
// 如果不传 page 和 limit, 或者传的是 page=a&limit=-100 将会有默认值 page=1&limit=15
@GetMapping("/demo")  // 如果参数不多的话, 不需要构建 dto, 直接写在这里就好了, 返回只有一个字段直接返回就好了, 也不用新建 vo
public JsonResult<PageInfo<DemoVo>> demoList(DemoDto dto, Page page) {
    // dto 中写方法来生成跟数据库对应的 model, 方法中可以先做 web 层的数据校验
    PageInfo<Demo> pageInfo = demoService.pageList(dto.demo(), page);
    return JsonResult.success("用户列表", DemoVo.assemblyData(pageInfo));
}
```

module-model 中的接口
```java
public interface DemoService {
    /** 获取分页数据 */   // dto 和 vo 是 controller 层的对象, 在 service 层使用跟数据库对应的 model 实体进行接收
    PageInfo<Demo> pageList(Demo param, Page page);
}
```

module-server 中的实现类
```java
@Service
public class DemoServiceImpl implements DemoService {

    private final DemoMapper demoMapper;
    // 用构造器来注入, 不用 @Autowired
    public DemoServiceImpl(DemoMapper demoMapper) {
        this.demoMapper = demoMapper;
    }

    @Override
    public PageInfo<Demo> pageList(Demo param, Page page) {
        DemoExample example = new DemoExample();
        if (U.isNotBlank(param)) {
            DemoExample.Criteria criteria = example.or();
            if (U.isNotBlank(param.getName())) {
                criteria.andNameLike(name);
            }
            // 如果有其他的属性 -> 动态拼接
        }
        // 下面的查询会自动基于 Page 对象中的 page 和 limit 值拼到 sql 语句中去, 也会自动添加 select count(*) 的查询
        List<Demo> demoList = demoMapper.selectByExample(example, Pages.param(page));
        // 上面的 List 包含了 select count(*) 的值, 使用 PageInfo 对返回对象二次封装一下
        return Pages.returnList(demoList);
    }
}
```


### 通过 Example 构建单表的 where 条件

如果我们想要生成如下的 sql 语句:
```sql
select xxx from `t_demo` where `name` = 'xx' and `level` > 1 and `ver` in (1, 2, 3)
```
可以这样构建  example 来达到上面的效果
```java
DemoExample demoExample = new DemoExample();
demoExample.or().andNameEqualTo("xx").andLevelGreaterThan(1).andVerIn(Arrays.asList(1, 2, 3));
demoMapper.selectByExample(DemoExample);
```

同样的 where 条件也可以用在 update 和 delete 上
```java
Demo demo = new Demo();
demo.setPassword("abc");

DemoExample demoExample = new DemoExample();
demoExample.or().andNameEqualTo("xx").andLevelGreaterThan(1).andVerIn(Arrays.asList(1, 2, 3));

demoMapper.updateByExampleSelective(demo, demoExample);
```
上面将会生成如下的 sql 语句
```sql
update `t_demo` set `password`='abc' where `name` = 'xx' and `level` > 1 and `ver` in (1, 2, 3)
```

如果要生成 or 语句, 可以像这样
```java
DemoExample demoExample = new DemoExample();
demoExample.or().andNameEqualTo("xx").andCreateTimeLessThan(new Date());
demoExample.or().andEmailEqualTo("xx").andCerIsNotNull();
demoExample.or().andPhoneEqualTo("xxx").andVerIn(Arrays.asList(1, 2, 3));
demoMapper.selectByExample(DemoExample);
```
生成的 sql 如下
```sql
select ... from `t_demo`
where (`name` = 'xx' and `create_time` < xxx)
   or (`email` = 'xx' and `cer` is not null)
   or (`phone` = 'xx' and `ver` in (1, 2, 3) )
```

如果要生成条件复杂的 or 语句(比如在一个 and 条件里面有好几个 or), exmple 将会无法实现, 此时就需要手写 sql 了.
当有一些不得不联表的 sql 语句, 或者基于 example 很难生成的 or 查询, 此时放在 custom.xml 中, 确保自动生成和手写的 sql 分开管理.

阿里的开发手册中也明确说明: **超过三个表禁止 join**

PS: 尽量不要使用 join 来联表, 尽量由应用程序来组装数据并每次向数据库发起单一且易维护的 sql 语句,
这样的好处是就算到了大后期, 对于数据库而言, 压力也全在单表的 sql 上, 优化起来很容易,
而且应用程序还可以在这里加上二级缓存, 将大部分的压力由 db 的 io 操作转移到了应用程序的内部运算和网卡的数据库连接上,
java 做内部运算本就是强项, 这一块成为瓶颈可能性很低且易重构, 数据库连接可以由 hikari 或 druid 连接池来达到高性能操作.


### 枚举映射
不管是在实体(数据库对应的 model), 还是前端过来的传输对象(dto), 或者返回给前端的显示对象(vo), 都可以直接用枚举来做为字段的类型

比如有这样一个 性别 的枚举
```java
/** 用户性别 */
public enum Gender {

    Male(1, "男"), Female(2, "女");

    int code;
    String value;
    Gender(int code, String value) {
        this.code = code;
        this.value = value;
    }

    /** 显示用 */
    public String getValue() {
        return value;
    }
    /** 数据关联用 */
    public int getCode() {
        return code;
    }

    /** 序列化给前端时, 如果只想给前端返回数值, 去掉此方法并把注解挪到 getCode 方法上即可 */
    @JsonValue
    public Map<String, String> serializer() {
        return U.serializerEnum(code, value);
    }
    /** 数据反序列化. 如 male、0、男、{"code": 0, "value": "男"} 都可以反序列化为 Gender.Male 值 */
    @JsonCreator
    public static Gender deserializer(Object obj) {
        return U.enumDeserializer(obj, Gender.class);
    }
}
```
其中 code 和 value 都要有, 分别用来存入数据库和显示, 每个模块的 test 中有 xxxGenerateEnumHandler 这个测试类,
运行后会在当前模块的 handler 包中生成对应的枚举处理类, 就像下面这样
```java
/**
* 当前 handle 是自动生成的
*
* @see org.apache.ibatis.type.TypeHandlerRegistry
* @see org.apache.ibatis.type.EnumTypeHandler
* @see org.apache.ibatis.type.EnumOrdinalTypeHandler
*/
public class GenderHandler extends BaseTypeHandler {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Gender parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public Gender getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return U.toEnum(Gender.class, rs.getObject(columnName));
    }

    @Override
    public Gender getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return U.toEnum(Gender.class, rs.getObject(columnIndex));
    }

    @Override
    public Gender getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return U.toEnum(Gender.class, cs.getObject(columnIndex));
    }
}
```
这个包下的所有类会被装载到 mybatis 的上下文中去, 这样在整个项目过程中, 任意地方都可以直接使用枚举而不需要基于数值转来转去


### 数据库相关的规范

1. 表名全部小写, 以 t_ 开头, 单词间用下划线隔开, 模块要包含在表名中, 如: 用户表 t_user, 用户信息表 t_user_info(一个库也很好区分模块)
2. 表要加上注释, 字符集用 utf8mb4, 使用 innodb 引擎, 如:  comment='xx' engine=InnoDB default charset=utf8mb4;
3. 字段要加上注释, 不允许为 null, 业务上可以为空的字段给定默认值, 如: \`type\` int not null default 0 comment 'xxx'
4. 会用到 text 字段的尽量抽成一个单表
5. 用这几种类型就可以了, 相关的表字段类型对应如下

| java 类型     | 数据库字段类型                                                                   |
| ------------- | ----------------------------------------------------------------------------- |
| Long          | 主键或外键或存到分的金额: bigint(20) unsigned not null default '0' comment '商品最低价(存到分)' |
| Integer、Enum | int not null default '0' comment '1 表示 x, 2 表示 x, 3 表示x'                   |
| Boolean       | tinyint(1) not null default '0' comment '1 表示已删除'                          |
| String        | varchar(16) not null default '' comment 'xx'  长度为 2 的幂次, 如 32 128 1024 等 |
| BigDecimal    | decimal(10,2) not null default '0' comment 'xxxx 金额, 可以用 long 即可'         |
| Date          | datetime not null default '0000-00-00 00:00:00' comment 'xxxxx 时间'           |

~
