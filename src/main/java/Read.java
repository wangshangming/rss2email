import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Date;

@Data
public class Read {
    private String title;
    private String link;
    @JSONField(format = "yyyy-MM-dd hh:mm:ss")
    private Date time;
}
