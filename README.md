## 框架介绍 

- 新一代Java开发框架，命名Phoenix，中文翻译为凤凰，火鸟。西方传说中可以浴火重生，这里使用寓意新生，重生的二代框架
- 总结了老框架被使用过程中遇见的各种问题，基于我们公司成员技术水平和项目特点，完全重新开发了一套
- 框架建立在spring boot，spring data jpa，quartz 的基础上，技术基础比较通用简单，方便大家学习和扩展

## 主要功能
 - 框架自带基于角色和状态自动机型的权限控制体系
 - 框架自带所有数据库实体对象的json映射和筛选方法，不需要写列表和详细页的后台接口，输出时不会引起递归的栈溢出
 - 框架自带完整的增删改的功能，不需要写接口，但需要设置权限
 - 框架自带统一的文件上传和图像处理接口，文件存储在aliyun的oss上
 - 框架自带完整的审计功能，可以查看历史的修改记录，包括改动的细节部分
 - 框架自带完整的群组管理体系，可在不同的维度对数据资源进行管理，监督和反馈
 - 框架自带Tag的体系，可以给所有实体对象加上Tag，例如用户画像
 - 框架自带多种渠道的即时通知和延时通知体系，包括站内信，企业微信，微信公众号，短信通知，不需要写代码，只要设置好相应的渠道模板
 - 框架自带完善的扩展接口，在不改动框架且代码量最小的情况下，程序员可以加入业务代码且不会影响框架功能
 
## 详细解释见本项目Wiki

## 项目成员 

-  产品经理: [[李扬]]
-  项目经理: [[李扬]]
-  开发人员: [[李扬]]
-  测试经理: 
-  运营负责: 无

## 代码规范

-  阿里的代码规范

## Python八荣八耻

- 以动手实践为荣 , 以只看不练为耻
- 以打印日志为荣 , 以单步跟踪为耻
- 以空格缩进为荣 , 以制表缩进为耻
- 以单元测试为荣 , 以人工测试为耻
- 以模块复用为荣 , 以复制粘贴为耻
- 以多态应用为荣 , 以分支判断为耻
- 以Pythonic为荣 , 以冗余拖沓为耻
- 以总结分享为荣 , 以跪求其解为耻
