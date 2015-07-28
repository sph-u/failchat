package failchat.sc2tv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import failchat.core.Smile;
import failchat.core.SmileManager;
import failchat.core.Source;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc2tvSmile extends Smile {
    public static final String SC2TV_SMILES_DIR_URL = "http://chat.sc2tv.ru/img/"; // используется для генерации url
    public static final String FILE_EXTENSION = ".png";

    private String fileName;

    // удаляем из кода символ :
    @Override
    @JsonProperty(value = "code")
    public void setCode(String code) {
        this.code = code.replaceAll("\\:", "");
    }

    @JsonProperty(value = "img")
    public void setImageUrl(String img) {
        this.fileName = img.split("\\?")[0]; // smile.png?1 -> smile.png
    }

    @Override
    public String getImageUrl() {
        return SC2TV_SMILES_DIR_URL + fileName;
    }

    @Override
    public Source getSource() {
        return Source.SC2TV;
    }

    @Override
    public String getCachePath() {
        return SmileManager.SMILES_DIR_REL.resolve(Source.SC2TV.getLowerCased())
                .resolve(code + FILE_EXTENSION).toString().replace('\\', '/');
    }

    @Override
    public String getFileName() {
        return code + FILE_EXTENSION;
    }
}
