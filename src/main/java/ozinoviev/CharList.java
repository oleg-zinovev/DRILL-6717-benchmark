package ozinoviev;

/**
 * @author ozinoviev
 * @since 05.09.18
 */
public enum CharList {
    ASCII("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"),
    NON_ASCII("абвгдежзиклмнопрстуфхцчшщъыьэюяАБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"),
    MIXED(ASCII.chars + NON_ASCII.chars);

    private final String chars;

    CharList(String chars) {
        this.chars = chars;
    }

    public String getChars() {
        return chars;
    }
}
