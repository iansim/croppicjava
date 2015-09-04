package com.common.controller;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Position;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Controller
public class ImageCropController {

    private Logger log = LoggerFactory.getLogger(getClass());
    private @Value("${temp.folder}") String tmpFolder;

    @RequestMapping(value = "/img_crop_to_file.php", method = RequestMethod.POST)
    public @ResponseBody String img_crop_to_file(
            @RequestParam("imgUrl") String imgUrl,
            @RequestParam("imgInitW") float imgInitW,
            @RequestParam("imgInitH") float imgInitH,
            @RequestParam("imgW") float imgW, @RequestParam("imgH") float imgH,
            @RequestParam("imgY1") final float imgY1,
            @RequestParam("imgX1") final float imgX1,
            @RequestParam("cropH") float cropH,
            @RequestParam("cropW") float cropW,
            @RequestParam("rotation") float rotation) {
        log.debug("calling img_crop_to_file.php");
        String tmpName = null;
        byte[] imgByte = null;
        if(imgUrl.indexOf("base64,")>0){
            imgByte = getImage(imgUrl);
            tmpName = UUID.randomUUID().toString() + getImageType(imgUrl);
        }else{
            File file = new File(tmpFolder+imgUrl.replace("images/" , ""));
            tmpName = UUID.randomUUID()+"."+FilenameUtils.getExtension(imgUrl);
            try {
                imgByte = FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        final float wr = imgInitW * 1.0f / imgW;
        final float hr = imgInitH * 1.0f / imgH;
        

        InputStream is = new ByteArrayInputStream(imgByte);
        try {
            //Get the rotated image
            BufferedImage bfImg = Thumbnails.of(is).rotate(rotation).scale(1.0).asBufferedImage();
            int rotated_width = bfImg.getWidth();
            int rotated_height = bfImg.getHeight();
            log.debug("imgInitW:"+imgInitW+" rotated_width:"+rotated_width);
            log.debug("imgInitH:"+imgInitH+" rotated_height:"+rotated_height);
            //Get the size difference between rotate image and init image
            final float dx = rotated_width - imgInitW;
            final float dy = rotated_height - imgInitH;
            
            //calculate the cut position
            Position pos = new Position() {
                @Override
                public Point calculate(int enclosingWidth, int enclosingHeight,
                        int width, int height, int insetLeft, int insetRight,
                        int insetTop, int insetBottom) {
                    return new Point(Math.round(dx/2)+Math.round((imgX1* wr)), Math.round(dy/2)+Math.round((imgY1*hr)));
                }
            };
            
            File f = new File(tmpFolder + File.separator + tmpName);
            f.createNewFile();
            
            Thumbnails
                    .of(bfImg)
                    .sourceRegion(pos, Math.round(cropW * wr),    
                            Math.round(cropH * hr))
                    .size(Math.round(cropW), Math.round(cropH))
                    .toFile(tmpFolder + File.separator + tmpName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> myMap = new HashMap<String, String>();
        myMap.put("status", "success");
        myMap.put("url", "images/" + tmpName);

        Gson gson = new GsonBuilder().create();
        return gson.toJson(myMap);
    }
    
    
    
    @RequestMapping(value = "/img_save_to_file.php", method = RequestMethod.POST)
    public @ResponseBody String img_save_to_file(@RequestParam("img") MultipartFile img) {
        
        
        String tmpName = UUID.randomUUID()+"."+FilenameUtils.getExtension(img.getOriginalFilename());
        BufferedImage bimg = null;
        try {
            bimg = Thumbnails.of(img.getInputStream()).scale(1).asBufferedImage();
            Thumbnails.of(bimg).scale(1).toFile(tmpFolder + File.separator + tmpName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, Object> myMap = new HashMap<String, Object>();
        myMap.put("status", "success");
        myMap.put("url", "images/" + tmpName);
        myMap.put("width", bimg.getWidth());
        myMap.put("height", bimg.getHeight());


        Gson gson = new GsonBuilder().create();
        return gson.toJson(myMap);
    }

    private byte[] getImage(String data) {
        String find = "base64,";
        String tokens = data.substring(data.indexOf(find) + find.length());
        String decoded = tokens.replaceAll("%2F", "/");
        byte[] bytes = Base64.decodeBase64(decoded);
        return bytes;
    }

    private static String getImageType(String data) {
        String start = "data:";
        String find = ";base64,";
        String type = data.substring(data.indexOf(start) + start.length(),
                data.indexOf(find));
        switch (type) {
        case "image/png":
            return ".png";
        case "image/jpeg":
            return ".jpeg";
        case "image/gif":
            return ".gif";
        default:
        }
        return type;
    }
}