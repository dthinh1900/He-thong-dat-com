package com.mealbooking.backend.adapter.media;

import com.mealbooking.backend.application.port.out.MenuImageInspector;
import com.mealbooking.backend.domain.error.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class ImageIoMenuInspector implements MenuImageInspector {
    @Override
    public String inspect(byte[] bytes) {
        try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(stream);
            if(readers.hasNext()) {
                ImageReader reader=readers.next();
                try {
                    reader.setInput(stream);
                    String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                    long pixels=(long)reader.getWidth(0)*reader.getHeight(0);
                    if(pixels<=0||pixels>20_000_000L) throw BusinessException.badRequest("Ảnh tối đa 20 megapixel.");
                    if(format.equals("png")||format.equals("jpeg")||format.equals("jpg")) {
                        reader.read(0); // reject corrupt images before saving
                        return format.equals("png")?"image/png":"image/jpeg";
                    }
                } finally { reader.dispose(); }
            }
        } catch(IOException|IllegalArgumentException e) { throw BusinessException.badRequest("Ảnh không hợp lệ."); }
        throw BusinessException.badRequest("Chỉ chấp nhận PNG hoặc JPEG hợp lệ.");
    }
}
