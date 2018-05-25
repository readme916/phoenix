package com.shangdao.phoenix.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.shangdao.phoenix.util.FileFormat;
import com.shangdao.phoenix.util.FileFormatJudge;
import com.shangdao.phoenix.util.HTTPResponse;
import com.shangdao.phoenix.util.OutsideRuntimeException;

@Service
public class FileUploadService {

	@Value("${spring.oss.endpoint}")
	private String endpoint;

	@Value("${spring.oss.accessKeyId}")
	private String accessKeyId;

	@Value("${spring.oss.accessKeySecret}")
	private String accessKeySecret;

	@Value("${spring.oss.bucketName}")
	private String bucketName;

	public HTTPResponse upload(MultipartFile file) {
		OssImage fileInformation = fileInformation(file);
		try {
			_upload(fileInformation, file.getInputStream());
			if (FileFormatJudge.isImage(fileInformation.getFileFormat())) {
				transferImage(fileInformation, file.getInputStream());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OutsideRuntimeException(8671, "文件上传到阿里云出错");
		}
		if (fileInformation.getSmall() != null) {
			return new HTTPResponse(0,"",fileInformation);
		} else {
			OssFile ossFile = new OssFile();
			BeanUtils.copyProperties(fileInformation, ossFile);
			return new HTTPResponse(0,"",ossFile);
		}
	}

	// ---------------------------------------------------------------------------------------------------------

	// 文件信息
	private OssImage fileInformation(MultipartFile file) {

		OssImage ossFile = new OssImage();
		try {
			ossFile.setOriginalFileName(file.getOriginalFilename());
			ossFile.setFileSize(file.getSize());

			// 新文件名
			String suffix = "";
			if (file.getOriginalFilename().lastIndexOf(".") != -1) {
				suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1)
						.toLowerCase();
			}
			UUID randomUUID = UUID.randomUUID();
			ossFile.setUuid(randomUUID.toString());
			// 文件类型
			String fileFormatString = suffix.toUpperCase();
			FileFormat realfileFormat = FileFormatJudge.getFormat(file.getInputStream());
			if (realfileFormat != null) {
				fileFormatString = realfileFormat.toString();
			}
			String newFileName = null;
			if (suffix.equals("")) {
				newFileName = randomUUID.toString() + "." + fileFormatString;
			} else {
				newFileName = randomUUID.toString() + "." + suffix;
			}
			ossFile.setNewFileName(newFileName);

			// 上传到阿里云的返回的文件地址

			String ossFileUrl = bucketName + "." + endpoint + "/" + newFileName;
			ossFile.setUrl(ossFileUrl);
			ossFile.setFileFormat(realfileFormat);

		} catch (IOException e) {
			e.printStackTrace();
			throw new OutsideRuntimeException(5672, "上传文件格式解析出错");
		}

		return ossFile;
	}

	// 文件上传，返回文件阿里云地址
	private OssImage _upload(OssImage fileInformation, InputStream fileInputStream) {
		OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
		try {
			ossClient.putObject(new PutObjectRequest(bucketName, fileInformation.getNewFileName(), fileInputStream));
		} catch (OSSException oe) {
			oe.printStackTrace();
			throw new OutsideRuntimeException(6121, oe.getErrorMessage());
		} finally {
			ossClient.shutdown();
		}
		return fileInformation;
	}

	private void transferImage(OssImage fileInformation, InputStream fileInputStream) {
		OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
		if (!FileFormatJudge.isImage(fileInformation.getFileFormat())) {
			return;
		}

		String transNewFileName = fileInformation.getUuid() + ".jpg";
		String transImageNameTemplate = fileInformation.getUuid() + "%s.jpg";

		try {
			if ("JPEG".equals(fileInformation.getFileFormat().toString())) {
				ImageInfo[] picutureTrans_198_720 = picutureResize_198_720(ossClient, transImageNameTemplate,
						fileInformation.getNewFileName(), fileInputStream);
				fileInformation.setSmall(picutureTrans_198_720[0]);
				fileInformation.setMiddle(picutureTrans_198_720[1]);
				fileInformation.setLarge(picutureTrans_198_720[2]);
				return;
			} else {
				GetObjectRequest request = picutureTrans(ossClient, "image/format,jpg",
						fileInformation.getNewFileName(), transNewFileName);
				ImageInfo[] picutureTrans_198_720 = picutureResize_198_720(ossClient, transImageNameTemplate,
						transNewFileName, ossClient.getObject(request).getObjectContent());
				fileInformation.setSmall(picutureTrans_198_720[0]);
				fileInformation.setMiddle(picutureTrans_198_720[1]);
				fileInformation.setLarge(picutureTrans_198_720[2]);
				return;
			}
		} catch (Exception e) {
			throw new OutsideRuntimeException(8462, "阿里云转换文件格式出错");
		} finally {
			ossClient.shutdown();
		}
	}

	private GetObjectRequest picutureTrans(OSSClient ossClient, String style, String primitiveFileName,
			String transNewFileName) {

		GetObjectRequest request = new GetObjectRequest(bucketName, primitiveFileName);
		// if(style!=null){
		request.setProcess(style);
		// }
		OSSObject ossObject = ossClient.getObject(request);
		InputStream inputStream = ossObject.getObjectContent();
		ossClient.putObject(new PutObjectRequest(bucketName, transNewFileName, inputStream));
		return request;
	}

	private ImageInfo[] picutureResize_198_720(OSSClient ossClient, String transFileNameTemplate,
			String transNewFileName, InputStream fileInputStream) {
		ImageInfo[] imageInfos = null;
		String ossFileReturnTemplate = bucketName + "." + endpoint + "/";
		try {
			BufferedImage bufferedImage = ImageIO.read(fileInputStream);
			Collection<Integer> colls = new ArrayList<Integer>();
			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();
			int size = 0;
			colls.add(height);
			colls.add(width);
			Integer min = Collections.min(colls);
			imageInfos = new ImageInfo[3];
			if (min >= 198) {
				GetObjectRequest request = picutureTrans(ossClient, "image/resize,m_mfit,w_198", transNewFileName,
						String.format(transFileNameTemplate, "_198"));
				imageInfos[0] = imageInfoCreate(3, 0, 0, 0,
						ossFileReturnTemplate + String.format(transFileNameTemplate, "_198"));
				if (min >= 720) {
					request = picutureTrans(ossClient, "image/resize,m_mfit,w_720", transNewFileName,
							String.format(transFileNameTemplate, "_720"));
					imageInfos[1] = imageInfoCreate(2, 0, 0, 0,
							ossFileReturnTemplate + String.format(transFileNameTemplate, "_720"));
				}
			}
			if (imageInfos[0] == null) {
				for (int i = 1; i < 4; i++) {
					imageInfos[i - 1] = imageInfoCreate(i, height, width, size,
							ossFileReturnTemplate + transNewFileName);
				}
			} else if (imageInfos[1] == null) {
				imageInfos[1] = imageInfoCreate(2, 0, 0, size,
						ossFileReturnTemplate + String.format(transFileNameTemplate, ""));
				imageInfos[2] = imageInfoCreate(1, height, width, size, ossFileReturnTemplate + transNewFileName);
			} else {
				imageInfos[2] = imageInfoCreate(1, height, width, size, ossFileReturnTemplate + transNewFileName);
			}
			return imageInfos;
		} catch (IOException e) {
			throw new OutsideRuntimeException(4721, "阿里云图片格式转换出错");
		}
		// return imageInfos;
	}

	private ImageInfo imageInfoCreate(Integer type, Integer height, Integer width, Integer size, String url) {
		ImageInfo imageInfo = new ImageInfo();
		imageInfo.setType(type);
		imageInfo.setHeight(height);
		imageInfo.setWidth(width);
		imageInfo.setSize(size);
		imageInfo.setUrl(url);
		return imageInfo;
	}

	public static class OssImage extends OssFile {
		private ImageInfo small;
		private ImageInfo middle;
		private ImageInfo large;

		public ImageInfo getSmall() {
			return small;
		}

		public void setSmall(ImageInfo small) {
			this.small = small;
		}

		public ImageInfo getMiddle() {
			return middle;
		}

		public void setMiddle(ImageInfo middle) {
			this.middle = middle;
		}

		public ImageInfo getLarge() {
			return large;
		}

		public void setLarge(ImageInfo large) {
			this.large = large;
		}
	}

	public static class OssFile{
		private String originalFileName;
		private String uuid;
		private String newFileName;
		private Long fileSize;
		private FileFormat fileFormat;
		private String url;

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#getOriginalFileName()
		 */
		public String getOriginalFileName() {
			return originalFileName;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#setOriginalFileName(java.lang.String)
		 */
		public void setOriginalFileName(String originalFileName) {
			this.originalFileName = originalFileName;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#getUuid()
		 */
		public String getUuid() {
			return uuid;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#setUuid(java.lang.String)
		 */
		public void setUuid(String uuid) {
			this.uuid = uuid;
		}


		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#getNewFileName()
		 */
		public String getNewFileName() {
			return newFileName;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#setNewFileName(java.lang.String)
		 */
		public void setNewFileName(String newFileName) {
			this.newFileName = newFileName;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#getFileSize()
		 */
		public Long getFileSize() {
			return fileSize;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#setFileSize(java.lang.Long)
		 */
		public void setFileSize(Long fileSize) {
			this.fileSize = fileSize;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#getFileType()
		 */
		public FileFormat getFileFormat() {
			return fileFormat;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#setFileType(com.shangdao.phoenix.util.FileFormat)
		 */
		public void setFileFormat(FileFormat fileType) {
			this.fileFormat = fileType;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#getUrl()
		 */
		public String getUrl() {
			return url;
		}

		/* (non-Javadoc)
		 * @see com.shangdao.phoenix.service.Itemp#setUrl(java.lang.String)
		 */
		public void setUrl(String url) {
			this.url = url;
		}

	}

	public static class ImageInfo {
		
		private Integer type;
		private Integer size;
		private Integer width;
		private Integer height;
		private String url;
		public Integer getType() {
			return type;
		}
		public void setType(Integer type) {
			this.type = type;
		}
		public Integer getSize() {
			return size;
		}
		public void setSize(Integer size) {
			this.size = size;
		}
		public Integer getWidth() {
			return width;
		}
		public void setWidth(Integer width) {
			this.width = width;
		}
		public Integer getHeight() {
			return height;
		}
		public void setHeight(Integer height) {
			this.height = height;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		
	}
}
