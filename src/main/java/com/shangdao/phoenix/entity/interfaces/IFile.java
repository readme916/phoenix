package com.shangdao.phoenix.entity.interfaces;

import java.util.Date;

import com.shangdao.phoenix.entity.act.Act;
import com.shangdao.phoenix.entity.example.Example;
import com.shangdao.phoenix.entity.example.ExampleLog;
import com.shangdao.phoenix.util.FileFormat;

public interface IFile<E extends ILogEntity,L extends ILog> extends IBaseEntity {
	E getEntity();
	void setEntity(E entity);

	Act getAct();
	void setAct(Act act);
	
	L getLog();
	void setLog(L log);
	
	String getOriginalFileName();

	void setOriginalFileName(String originalFileName);

	String getNewFileName();

	void setNewFileName(String newFileName);

	long getFileSize();

	void setFileSize(long fileSize);

	FileFormat getFileFormat();

	void setFileFormat(FileFormat fileType);

	String getUrl();

	void setUrl(String url);
	
	int getWidth();
	void setWidth(int width);
	
	int getHeight();
	void setHeight(int height);
	
	String getSmallImage();
	void setSmallImage(String smallImage);
	String getMiddleImage();
	void setMiddleImage(String middleImage);
	String getLargeImage();
	void setLargeImage(String largeImage);
	
}