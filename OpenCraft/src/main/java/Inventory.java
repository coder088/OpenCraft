public class Inventory {

    private ItemStack[] hotbar = new ItemStack[9];
    private int selectedSlot;

    public Inventory() {
        this.hotbar[0] = new ItemStack((byte)1, 64);//lets make the player start with a few blocks
        this.hotbar[2] =  new ItemStack((byte)2,32);
    }

    public void setSelectedSlot(int n){selectedSlot = n;}


}
