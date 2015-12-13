package logbook.api;

/**
 * {@code CAPIListenerSpi}が受け入れるAPIのURIを表す注釈です。
 */
public @interface API {

    /**
     * APIのURIを返します
     *
     * @return APIのURI
     */
    String[] values();

}