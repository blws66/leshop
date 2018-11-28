package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadService {
    @Autowired
    private FastFileStorageClient fileStorageClient;

    private static final List<String> CONTENT_TYPE= Arrays.asList("image/png", "image/jpeg");

    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

    public String uploadImage(MultipartFile file) {
        try {
            //图片信息校验
            String contentType = file.getContentType();
            if(!CONTENT_TYPE.contains(contentType)){
                logger.info("上传图片失败文件类型不匹配：｛｝",contentType);
                return null;
            }
            //判断图片内容是否符合要求
            BufferedImage image = ImageIO.read(file.getInputStream());
            if(image==null){
                logger.info("上传失败，文件内容不符合要求");
                return null;
            }
//            //保存图片至本地
//            file.transferTo(new File("E:\\idealwookspace\\leyou\\image-leyou\\"+file.getOriginalFilename()));
//            保存图片到服务器
            String ext = StringUtils.substringAfterLast(file.getOriginalFilename(),".");
            StorePath storePath = this.fileStorageClient.uploadFile(file.getInputStream(), file.getSize(), ext, null);

            //获取图片路径
            String url = "http://image.leyou.com/"+storePath.getFullPath();
            return url;
        } catch (IOException e) {
            return null;
        }
    }
}
