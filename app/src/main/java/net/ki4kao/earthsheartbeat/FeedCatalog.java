package net.ki4kao.earthsheartbeat;

/**
 * The catalog of feeds shown on ki4kao.net/eh and selectable for the widget.
 * Each feed carries its source URL and the station it belongs to. The
 * Referer required to defeat each host's hotlink protection is derived from
 * the URL host in {@link WidgetUpdater}.
 */
public final class FeedCatalog {

    public static final class Feed {
        public final String name;
        public final String station;
        public final String url;
        Feed(String name, String station, String url) {
            this.name = name;
            this.station = station;
            this.url = url;
        }
    }

    // Order roughly matches the eh/index.html layout. Schumann-type feeds first
    // within each station so the widget picker surfaces them prominently.
    public static final Feed[] FEEDS = new Feed[] {
        // Cumiana Observatory — vlf.it (Italy)
        new Feed("Plotted Spectrum",          "Cumiana · vlf.it (IT)", "http://www.vlf.it/cumiana/last-plotted.jpg"),
        new Feed("Lot-et-Garonne Coil 8h",    "Cumiana · vlf.it (IT)", "http://www.vlf.it/cumiana/lotetgaronne_last-coil_8h.jpg"),
        new Feed("Sos Enattos Mine",          "Cumiana · vlf.it (IT)", "http://www.vlf.it/cumiana/last-sosenattos-mine.jpg"),
        new Feed("Virgo RDF",                 "Cumiana · vlf.it (IT)", "http://www.vlf.it/cumiana/last-virgo-rdf.jpg"),
        new Feed("Virgo LR",                  "Cumiana · vlf.it (IT)", "http://www.vlf.it/cumiana/last-virgo-LR.jpg"),
        new Feed("E-VLF",                     "Cumiana · vlf.it (IT)", "http://www.vlf.it/cumiana/last_E-VLF.jpg"),
        new Feed("GEOMAR",                    "Cumiana · vlf.it (IT)", "http://www.vlf.it/cumiana/last-geomar.jpg"),
        new Feed("Marconi Multistrip Slow",   "Cumiana · vlf.it (IT)", "http://www.vlf.it/cumiana/last-marconi-multistrip-slow.jpg"),

        // Etna ERO — etna-ero.it (Italy)
        new Feed("Etna Coil 8h",              "Etna ERO · etna-ero.it (IT)", "http://www.etna-ero.it/live_etna/last-coil_8h.jpg"),
        new Feed("Etna Coil 1h",              "Etna ERO · etna-ero.it (IT)", "http://www.etna-ero.it/live_etna/last-coil_1h.jpg"),
        new Feed("Etna Ortholoop 2h VLF",     "Etna ERO · etna-ero.it (IT)", "http://www.etna-ero.it/live_etna/last-ortholoop_2h_vlf.jpg"),

        // SOS70 — sos70.ru (Russia)
        new Feed("Schumann (SHM)",            "SOS70 · sos70.ru (RU)", "https://sos70.ru/provider.php?file=shm.jpg"),
        new Feed("SRF",                       "SOS70 · sos70.ru (RU)", "https://sos70.ru/provider.php?file=srf.jpg"),
        new Feed("UMF",                       "SOS70 · sos70.ru (RU)", "https://sos70.ru/provider.php?file=umf.jpg"),
        new Feed("X-ray (Cmx)",               "SOS70 · sos70.ru (RU)", "https://sos70.ru/provider.php?file=xraycmx.jpg"),

        // NCK Observatory — nckobs.hu (Hungary)
        new Feed("SR Dynamic Spectrum",       "NCK · nckobs.hu (HU)", "https://nckobs.hu/data/sr/SR_dynspec_codemo_latest.png"),
    };

    public static Feed byUrl(String url) {
        if (url == null) return FEEDS[0];
        for (Feed f : FEEDS) if (f.url.equals(url)) return f;
        return FEEDS[0];
    }

    private FeedCatalog() {}
}
