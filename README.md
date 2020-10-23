
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


### 分页示例

在 controller 中这样
```java
@RestController
@RequestMapping("/demo")
public class DemoController {

    // 如果不传 page 和 limit, 或者传的是 page=a&limit=-100 这种时 Page 将会有默认值 page=1&limit=10
    // 好的实践是每个接口都有各自的 dto 和 vo, 如果参数不多则不需要构建 dto, 返回只有一个字段也不用新建 vo
    @GetMapping
    public JsonResult<PageInfo<DemoVo>> demoList(DemoDto dto, Page page) {
        // dto 和 vo 是 controller 层的对象, 在 service 层使用跟数据库对应的 model 实体进行接收和返回
        PageInfo<Demo> pageInfo = demoService.pageList(dto.demo(), page);
        return JsonResult.success("用户列表", DemoVo.assemblyData(pageInfo));
    }
}
```

module-model 中的接口
```java
public interface DemoService {
    /** 获取分页数据 */
    PageInfo<Demo> pageList(Demo param, Page page);
}
```

module-server 中的实现类
```java
@Service
@AllArgsConstructor
public class DemoServiceImpl implements DemoService {

    private final DemoMapper demoMapper;

    @Override
    public PageInfo<Demo> pageList(Demo param, Page page) {
        // select xxx, yyy from demo where name like '%xxxxx%' 其中 name 的条件是动态的(不为空才拼接)
        // 会自动运行 select count(*), 如果结果为 0 则不会去执行 limit 
        return Pages.returnList(demoMapper.selectPage(Pages.param(page), Wrappers.lambdaQuery(Demo.class)
                  .like(U.isNotBlank(param.getName()), Demo::getName, param.getName())));
    }
}
```


### 数据库相关的规范

1. 表名全部小写, 以 t_ 开头, 单词间用下划线隔开, 模块要包含在表名中, 如: 用户表 t_user, 用户信息表 t_user_info(一个库也很好区分模块)
2. 表要加上注释, 字符集用 utf8mb4, 使用 innodb 引擎, 如:  comment='xx' engine=InnoDB default charset=utf8mb4;
3. 字段要加上注释, 不允许为 null, 业务上可以为空的字段给定默认值, 如: \`type\` int not null default 0 comment 'xxx'
4. 会用到 text 字段的尽量抽成一个单表
5. 用这几种类型就可以了, 相关的表字段类型对应如下

| java 类型     | 数据库字段类型                                                                               |
| ------------- | ------------------------------------------------------------------------------------------ |
| Long          | 主键或外键或存到分的金额: bigint(20) unsigned not null default '0' comment '商品最低价(存到分)' |
| Integer、Enum | int not null default '0' comment '1 表示 x, 2 表示 x, 3 表示x'                               |
| Boolean       | tinyint(1) not null default '0' comment '1 表示已删除'                                      |
| String        | varchar(16) not null default '' comment 'xx'  长度为 2 的幂次, 如 32 128 1024 等             |
| BigDecimal    | decimal(10,2) not null default '0' comment 'xxxx 金额, 可以用 long 即可'                     |
| Date          | datetime not null default '0000-00-00 00:00:00' comment 'xxxxx 时间'                        |

~
