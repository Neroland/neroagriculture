package za.co.neroland.neroagriculture.catalog;

/** Lower priority number wins. */
public enum CatalogSource {
    DATAPACK(0), CONFIG(1), METEOR(2), BUILTIN(3), ORE_TAG(4);
    private final int priority;
    CatalogSource(int priority) { this.priority = priority; }
    public int priority() { return priority; }
}
