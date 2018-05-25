package com.shangdao.phoenix.entity.interfaces;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.shangdao.phoenix.service.FileUploadService.OssImage;

public interface ILogEntity<L extends ILog,F extends IFile,N extends INoticeLog> extends IBaseEntity{
	public Set<L> getLogs();
	public Set<F> getFiles();
	public Set<N> getNotices();
	public Date getLastModifiedAt();
	public List<OssImage> getUploadFiles();
	public String getNote();
}
