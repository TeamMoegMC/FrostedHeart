package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.widget.ScrollBarWidget;
import com.teammoeg.chorda.client.widget.TabImageButton;
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class TownManagerScreen extends Screen {
    public static ResourceLocation TEXTURE = FHClientUtils.makeGuiTextureLocation("town_manage_screen");
    public static final int RESIDENT_TAB = 0;
    public static final int BLOCK_TAB = 1;

    public int imageWidth, imageHeight, leftPos, topPos;
    private ScreenMode activeScreenMode;
    public TeamTown town;

    protected TownManagerScreen(Component pTitle) {
        super(pTitle);
        this.town = CClientTeamDataManager.INSTANCE.getInstance().getData(FHSpecialDataTypes.TOWN_DATA).createTeamTown();
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        if(town != null){
            if(this.activeScreenMode == null){
                setScreenMode(getResidentListScreenMode());
            } else{
                //这里应该调用所有会存在Screen里的Mode的初始化方法，但是现在只有这一个的init方法需要调用，所以只调用这个。
                if(blockListScreenMode != null) blockListScreenMode.init();
                if(blockInfoScreenMode != null) blockInfoScreenMode.init();
                if(residentInfoScreenMode != null) residentInfoScreenMode.init();
                if(residentListScreenMode != null) residentListScreenMode.init();
                if(activeScreenMode != null) activeScreenMode.init();

                activeScreenMode.onEnter();
            }
        } else {
            setScreenMode(getNoTownScreenMode());
        }
        for(int tabIndex = 0; tabIndex < 2; tabIndex++){
            int x = leftPos - 22;
            int y = topPos + tabIndex * (18 + 2) +2;
            TabImageButton tabImageButton;
            switch(tabIndex){
                case RESIDENT_TAB:{
                    tabImageButton = new TabImageButton(TEXTURE, x, y, 22, 18, 180, 59, tabIndex, pButton ->  setScreenMode(getResidentListScreenMode())).bind(() -> activeScreenMode.getTabBelongTo());
                    System.out.println("duck_egg debug: adding tabImageButton of resident tab");
                    break;
                }
                case BLOCK_TAB:{
                    tabImageButton = new TabImageButton(TEXTURE, x, y, 22, 18, 180, 59, tabIndex, pButton ->  setScreenMode(getBlockListScreenMode())).bind(() -> activeScreenMode.getTabBelongTo());
                    System.out.println("duck_egg debug: adding tabImageButton of block tab");
                    break;
                }
                default :
                    throw new IllegalArgumentException("Unexpected value: " + tabIndex);
            }
            this.addRenderableWidget(tabImageButton);
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        this.activeScreenMode.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    public void renderBackground(GuiGraphics pGuiGraphics){
        pGuiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    private void setScreenMode(ScreenMode screenMode){
        //没有Town时无法切换，避免错误
        if(screenMode instanceof NoTownScreenMode){
            return;
        }
        if(this.activeScreenMode != null){
            this.activeScreenMode.onExit();
            System.out.println("duck_egg debug: changing actingScreenMode from " + this.activeScreenMode.getClass().getName());
        }
        this.activeScreenMode = screenMode;
        this.activeScreenMode.onEnter();
        System.out.println("duck_egg debug: changing actingScreenMode to " + this.activeScreenMode.getClass().getName());
    }

    public interface ScreenMode{

        default void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick){};

        //获取该模式属于哪个tab，用于决定Tab按钮的显示方式
        int getTabBelongTo();

        void onEnter();

        void onExit();

        void init();

    }

    private NoTownScreenMode noTownScreenMode;
    private NoTownScreenMode getNoTownScreenMode(){
        if(noTownScreenMode == null) {
            noTownScreenMode = new NoTownScreenMode();
        }
        return noTownScreenMode;
    }
    class NoTownScreenMode implements ScreenMode{
        @Override
        public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            //todo:目前这些translatable的key都是乱填的，做好之后需要改成FH对应的格式并加上翻译
            pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.no_town"), leftPos + 10, topPos + 10, 0xFFFFFF);
        }

        @Override
        public int getTabBelongTo() {
            return -1;
        }

        @Override
        public void onEnter() {
            FHMain.LOGGER.error("TownMangerScreen ERROR: Town is null!");
        }

        @Override
        public void onExit() {

        }

        /**
         * if this method is called together with onEnter(), call this first.
         */
        @Override
        public void init() {

        }
    }


    private BlockListScreenMode blockListScreenMode;
    private BlockListScreenMode getBlockListScreenMode(){
        if(blockListScreenMode == null) {
            blockListScreenMode = new BlockListScreenMode();
        }
        return blockListScreenMode;
    }
    class BlockListScreenMode implements ScreenMode{

        //由于这个的会随Scrollbar改变，直接以Widget形式加入
        List<Button> blockButtons;

        ScrollBarWidget scrollBar;

        //当前可见的行数
        int totalVisibleRows = imageHeight / 20;

        BlockListScreenMode(){
            this.scrollBar = new ScrollBarWidget(leftPos + imageWidth - 10, topPos, 10, imageHeight, town.getTownBlocks().size() - totalVisibleRows + 1)
                    .setOnRowChanged(this::onRowChanged)
                    /*.setThumbTexture(TEXTURE)todo:之后再加*/;
            this.blockButtons = new ArrayList<>();
            int i = 0;
            for(TownWorkerData workerData : town.getTownBlocks().values()){
                Button button = new Button.Builder(Component.literal( i+1 + workerData.getType().toString()), (pButton -> {
                    setScreenMode(new BlockInfoScreenMode(workerData));
                }))
                        .size(imageWidth - 10, 20)
                        .pos(leftPos, topPos + 20 * i)
                        .build();
                blockButtons.add(button);
                i++;
                System.out.println("duck_egg debug: adding block button for: " + workerData);
                System.out.println("duck_egg debug: block button pos: " + button.getX() + " " + button.getY());
            }
        }

        public void onRowChanged(int rowOld, int rowNew) {
            if (rowOld == rowNew) return;

            int oldEnd = Math.min(rowOld + totalVisibleRows, blockButtons.size());
            int newEnd = Math.min(rowNew + totalVisibleRows, blockButtons.size());

            // 计算需要移除和添加的范围
            int removeStart = (rowNew > rowOld) ? rowOld : newEnd;
            int removeEnd = (rowNew > rowOld) ? Math.min(rowNew, oldEnd) : oldEnd;

            int addStart = (rowNew > rowOld) ? Math.max(oldEnd, rowNew) : rowNew;
            int addEnd = (rowNew > rowOld) ? newEnd : Math.min(rowOld, newEnd);

            // 移除不再可见的按钮
            for (int i = removeStart; i < removeEnd; i++) {
                removeWidget(blockButtons.get(i));
            }

            // 添加新可见的按钮并更新所有可见按钮的位置
            for (int i = rowNew; i < newEnd; i++) {
                int relativeRow = i - rowNew;
                Button button = blockButtons.get(i);
                button.setY(topPos + 20 * relativeRow);
                if (i >= addStart && i < addEnd) {
                    addRenderableWidget(button);
                }
            }
        }

        @Override
        public int getTabBelongTo() {
            return TownManagerScreen.BLOCK_TAB;
        }

        @Override
        public void onEnter() {
            addRenderableWidget(scrollBar);
            int currentRow = scrollBar.getCurrentRow();
            for(int i = currentRow; i < currentRow + totalVisibleRows && i < blockButtons.size() ; i++){
                addRenderableWidget(blockButtons.get(i));
            }
        }

        @Override
        public void onExit() {
            removeWidget(scrollBar);
            int currentRow = scrollBar.getCurrentRow();
            for(int i = currentRow; i < currentRow + totalVisibleRows && i < blockButtons.size() ; i++){
                removeWidget(blockButtons.get(i));
            }
        }

        @Override
        public void init() {
            //刷新可见行数
            this.totalVisibleRows = imageHeight / 20;
            //刷新ScrollBar
            if(scrollBar != null){
                int currentRow = scrollBar.getCurrentRow();
                this.scrollBar = new ScrollBarWidget(leftPos + imageWidth - 10, topPos, 10, imageHeight, town.getTownBlocks().size() - totalVisibleRows + 1)
                        .setOnRowChanged(this::onRowChanged)
                /*.setThumbTexture(TEXTURE)todo:之后再加*/;
                scrollBar.setCurrentRow(currentRow);
            } else {
                this.scrollBar = new ScrollBarWidget(leftPos + imageWidth - 10, topPos, 10, imageHeight, town.getTownBlocks().size() - totalVisibleRows + 1)
                        .setOnRowChanged(this::onRowChanged)
                /*.setThumbTexture(TEXTURE)todo:之后再加*/;
            }
            //刷新所有按钮X坐标
            for(Button button : blockButtons){
                button.setX(leftPos);
            }
            //刷新可见按钮Y坐标
            for(int i = scrollBar.getCurrentRow(); i < scrollBar.getCurrentRow() + totalVisibleRows && i < blockButtons.size(); i++){
                Button button = blockButtons.get(i);
                button.setY(topPos + 20 * (i - scrollBar.getCurrentRow()));
            }
        }
    }

    private BlockInfoScreenMode getBlockInfoScreenMode(TownWorkerData data){
        if(blockInfoScreenMode == null){
            blockInfoScreenMode = new BlockInfoScreenMode(data);
        } else{
            blockInfoScreenMode.workerData = data;
        }
        return blockInfoScreenMode;
    }
    private BlockInfoScreenMode blockInfoScreenMode;
    class BlockInfoScreenMode implements ScreenMode{

        //城镇方块的workerData
        TownWorkerData workerData;

        Button backButton = new Button.Builder(Component.literal("Back"/*todo: Translation Key*/)//todo: 应为ImageButton，弄个往左指的箭头
                , (pButton -> setScreenMode(getBlockListScreenMode())))
                .size(10, 10)
                .pos(leftPos + 10, topPos + 10)
                .build();

        BlockInfoScreenMode(TownWorkerData data){
            workerData = data;
        }

        @Override
        public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if(workerData == null){
                pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.no_block_selected"), leftPos + 10, topPos + 20, 0xFFFFFF);
                return;
            }
            pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.block_type").append(" : " + workerData.getType().toString()), leftPos + 10, topPos + 20, 0xFFFFFF);
            pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.block_pos").append(" : " + workerData.getPos().toString()), leftPos + 10, topPos + 40, 0xFFFFFF);
            pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.work_details"), leftPos + 10, topPos + 60, 0xFFFFFF);
        }

        @Override
        public int getTabBelongTo() {
            return BLOCK_TAB;
        }

        @Override
        public void onEnter() {
            addRenderableWidget(backButton);
        }

        @Override
        public void onExit() {
            removeWidget(backButton);
        }

        @Override
        public void init() {
            this.backButton.setX(leftPos + 10);
            this.backButton.setY(topPos + 10);
        }
    }

    private ResidentListScreenMode getResidentListScreenMode(){
        if(residentListScreenMode == null){
            residentListScreenMode = new ResidentListScreenMode();
        }
        return residentListScreenMode;
    }
    private ResidentListScreenMode residentListScreenMode;
    class ResidentListScreenMode implements ScreenMode{

        //由于这个的会随Scrollbar改变，直接以Widget形式加入
        List<Button> residentButtons;

        ScrollBarWidget scrollBar;

        //当前可见的行数
        int totalVisibleRows = imageHeight / 20;

        ResidentListScreenMode(){
            this.scrollBar = new ScrollBarWidget(leftPos + imageWidth - 10, topPos, 10, imageHeight, town.getAllResidents().size() - totalVisibleRows + 1)
                    .setOnRowChanged(this::onRowChanged)
                    /*.setThumbTexture(TEXTURE)todo:之后再加*/;
            this.residentButtons = new ArrayList<>();
            int i = 0;
            for(Resident resident : town.getAllResidents()){
                Button button = new Button.Builder(Component.literal(i+1 + " " + resident.getFirstName() + " " + resident.getLastName()), (pButton -> {
                    setScreenMode(getResidentInfoScreenMode(resident));
                }))
                        .size(imageWidth - 10, 20)
                        .pos(leftPos, topPos + 20 * i)
                        .build();
                residentButtons.add(button);
                i++;
                System.out.println("duck_egg debug: adding resident button for: " + resident.getFirstName() + " " + resident.getLastName());
                System.out.println("duck_egg debug: resident button pos: " + button.getX() + " " + button.getY());
            }
        }

        public void onRowChanged(int rowOld, int rowNew) {
            if (rowOld == rowNew) return;

            int oldEnd = Math.min(rowOld + totalVisibleRows, residentButtons.size());
            int newEnd = Math.min(rowNew + totalVisibleRows, residentButtons.size());

            // 计算需要移除和添加的范围
            int removeStart = (rowNew > rowOld) ? rowOld : newEnd;
            int removeEnd = (rowNew > rowOld) ? Math.min(rowNew, oldEnd) : oldEnd;

            int addStart = (rowNew > rowOld) ? Math.max(oldEnd, rowNew) : rowNew;
            int addEnd = (rowNew > rowOld) ? newEnd : Math.min(rowOld, newEnd);

            // 移除不再可见的按钮
            for (int i = removeStart; i < removeEnd; i++) {
                removeWidget(residentButtons.get(i));
            }

            // 添加新可见的按钮并更新所有可见按钮的位置
            for (int i = rowNew; i < newEnd; i++) {
                int relativeRow = i - rowNew;
                Button button = residentButtons.get(i);
                button.setY(topPos + 20 * relativeRow);
                if (i >= addStart && i < addEnd) {
                    addRenderableWidget(button);
                }
            }
        }

        @Override
        public int getTabBelongTo() {
            return RESIDENT_TAB;
        }

        @Override
        public void onEnter() {
            addRenderableWidget(scrollBar);
            int currentRow = scrollBar.getCurrentRow();
            for(int i = currentRow; i < currentRow + totalVisibleRows && i < residentButtons.size() ; i++){
                addRenderableWidget(residentButtons.get(i));
            }
        }

        @Override
        public void onExit() {
            removeWidget(scrollBar);
            int currentRow = scrollBar.getCurrentRow();
            for(int i = currentRow; i < currentRow + totalVisibleRows && i < residentButtons.size() ; i++){
                removeWidget(residentButtons.get(i));
            }
        }

        @Override
        public void init() {
            //刷新可见行数
            this.totalVisibleRows = imageHeight / 20;
            //刷新ScrollBar
            if(scrollBar != null){
                int currentRow = scrollBar.getCurrentRow();
                this.scrollBar = new ScrollBarWidget(leftPos + imageWidth - 10, topPos, 10, imageHeight, town.getAllResidents().size() - totalVisibleRows + 1)
                        .setOnRowChanged(this::onRowChanged)
                /*.setThumbTexture(TEXTURE)todo:之后再加*/;
                scrollBar.setCurrentRow(currentRow);
            } else {
                this.scrollBar = new ScrollBarWidget(leftPos + imageWidth - 10, topPos, 10, imageHeight, town.getAllResidents().size() - totalVisibleRows + 1)
                        .setOnRowChanged(this::onRowChanged)
                /*.setThumbTexture(TEXTURE)todo:之后再加*/;
            }
            //刷新所有按钮X坐标
            for(Button button : residentButtons){
                button.setX(leftPos);
            }
            //刷新可见按钮Y坐标
            for(int i = scrollBar.getCurrentRow(); i < scrollBar.getCurrentRow() + totalVisibleRows && i < residentButtons.size(); i++){
                Button button = residentButtons.get(i);
                button.setY(topPos + 20 * (i - scrollBar.getCurrentRow()));
            }
        }
    }


    private ResidentInfoScreenMode getResidentInfoScreenMode(Resident resident){
        if(residentInfoScreenMode == null){
            residentInfoScreenMode = new ResidentInfoScreenMode(resident);
        } else{
            residentInfoScreenMode.resident = resident;
        }
        return residentInfoScreenMode;
    }
    private ResidentInfoScreenMode residentInfoScreenMode;
    class ResidentInfoScreenMode implements ScreenMode{
        Resident resident;

        Button backButton = new Button.Builder(Component.literal("Back"/*todo: Translation Key*/)//todo: 应为ImageButton，弄个往左指的箭头
                , (pButton -> setScreenMode(getResidentListScreenMode())))
                .size(10, 10)
                .pos(leftPos + 10, topPos + 10)
                .build();

        ResidentInfoScreenMode(Resident resident){
            this.resident = resident;
        }

        @Override
        public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if(resident == null){
                pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.no_resident_selected"), leftPos + 10, topPos + 20, 0xFFFFFF);
                return;
            }
            pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.resident_name").append(" : " + resident.getFirstName() + " " + resident.getLastName()), leftPos + 10, topPos + 20, 0xFFFFFF);
            pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.resident_health").append(" : " + resident.getHealth()), leftPos + 10, topPos + 40, 0xFFFFFF);
            pGuiGraphics.drawString(font, Component.translatable("frostedheart.town_manager.resident_mental").append(" : " + resident.getMental()), leftPos + 10, topPos + 60, 0xFFFFFF);
            //todo: more info need to be added, maybe second page needed? I don't know...
        }

        @Override
        public int getTabBelongTo() {
            return RESIDENT_TAB;
        }

        @Override
        public void onEnter() {
            addRenderableWidget(backButton);
        }

        @Override
        public void onExit() {
            removeWidget(backButton);
        }

        @Override
        public void init() {
            this.backButton.setX(leftPos + 10);
            this.backButton.setY(topPos + 10);
        }
    }
}
