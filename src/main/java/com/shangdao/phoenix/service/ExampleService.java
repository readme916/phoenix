package com.shangdao.phoenix.service;

import com.shangdao.phoenix.entity.department.Department;
import com.shangdao.phoenix.entity.example.Example;
import com.shangdao.phoenix.service.GetMethodService.GetMethodWrapper;
import com.shangdao.phoenix.service.PostMethodService.PostMethodWrapper;
import com.shangdao.phoenix.util.CommonUtils;
import com.shangdao.phoenix.util.HTTPResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExampleService implements InterfaceEntityService {

    //保证bean创建顺序，没实际用处
    @Autowired
    private InitService initService;

    public HTTPResponse list(GetMethodWrapper getMethodWrapper, HTTPResponse response) {
        List<Map> list = (List) (response.getData());
        System.out.println("返回之前，处理一波结果");
//		throw new OutsideRuntimeException(122,"测试");
        return response;
    }

    public HTTPResponse detail(GetMethodWrapper getMethodWrapper, HTTPResponse response) {
        Map map = (Map) response.getData();
        System.out.println("返回之前，处理一波结果");
        return response;
    }

    public HTTPResponse group(GetMethodWrapper getMethodWrapper, HTTPResponse response) {
        Map map = (Map) (response.getData());
        System.out.println("返回之前，处理一波结果");
//		throw new OutsideRuntimeException(122,"测试");
        return response;
    }

    //最好不要修改oldInstance
    public void update(PostMethodWrapper postMethodWrapper, Object postBody, Object oldInstance) {
        Example post = (Example) postBody;
        Example old = (Example) oldInstance;
        Set<Department> departments = post.getDepartments();
        //如果没有提交部门，部门只增加不减少
        if (departments == null) {
            departments = new HashSet<Department>();
            departments.addAll(old.getDepartments());
        }
        departments.addAll(CommonUtils.currentUser().getDepartments());
        post.setDepartments(departments);
//		post.setText("kkkkkkkkkkkkkkkkkkkkkkkk");
        System.out.println("根据old的实体和业务逻辑，改变postObject后,包括其他数据库操作");
    }

    //最好不要修改oldInstance
    public void create(PostMethodWrapper postMethodWrapper, Object postBody, Object oldInstance) {
        Example post = (Example) postBody;
//		Example old = null;
//		post.setText("kkkkkkkkkkkkkkkkkkkkkkkk");
        System.out.println("根据old的实体和业务逻辑，改变postObject后,包括其他数据库操作");
    }

    @Override
    @PostConstruct
    public void registerService() {
        initService.getStructure(Example.class).setEntityService(this);

    }
}
