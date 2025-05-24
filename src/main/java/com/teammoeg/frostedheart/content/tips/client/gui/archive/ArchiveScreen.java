package com.teammoeg.frostedheart.content.tips.client.gui.archive;

import com.teammoeg.chorda.client.cui.PrimaryLayer;

public final class ArchiveScreen extends PrimaryLayer {
    // Home / Category / SubCategory / Entry
    // category list1
    // -> sub category list1
    // -> sub category list2
    //    -> entry1
    //    -> entry2
    //    -> entry3
    // category list2

    public static String path;
    public final DetailBox detailBox;
    public final CategoryBox categoryBox;

    public ArchiveScreen() {
        this.detailBox = new DetailBox(this);
        detailBox.fillContent(
                detailBox.text("&#9294A3CRh:SP 纳米机器人集群简史").setTitle(0, 2),
                detailBox.text("&0集群的诞生").setTitle(0xFFC1E52F, 2),
                detailBox.text("在纳米机器人技术的研发初期，如何控制一群比尘芥还要微小百倍、并且数量动态变化的个体是让人颇为头疼的工程学问题。"),
                detailBox.text("2165年，Felicia Wâpanatâhk 提出了一种模型，描述了纳米机器人个体间相互作用的数学结构，并引入了“集群”“子集群”的层级划分。该模型奠定了纳米机器人技术的控制论基础，并引发了快速的技术迭代。值得一提的是，Wâpanatâhk 在2179年因提出集群模型获得了诺贝尔物理学奖——那正是诺贝尔奖委员会宣告破产的前一年。"),
                detailBox.text("经过数十年的发展，各大企业几乎已经达成了有关纳米机器人集群设计的共识。如今的所有集群设计都包含数个接收控制信号的信使子集群、递送控制信号的狼烟子集群及控制个体数量的演化子集群。其他类型的子集群则取决于具体的功能设计。"),
                detailBox.emptyLine(),
                detailBox.text("&0形态学").setTitle(0xFFC1E52F, 2),
                detailBox.text("“匍匐集群”").setQuote(0xFFC1E52F),
                detailBox.image("minecraft:textures/item/apple.png").setBackgroundColor(0xFF585966),
                detailBox.text("几乎可以认为黏浆状的“匍匐集群”是大部分原型系列采取的集群形态，除了那时的纳米机器人集群还不似现在这样黏。"),
                detailBox.text("早期设计中的“匍匐集群”可以轻松爬上陡峭的洞穴壁，让有机基质中的纳米机器人个体吸收并分解其中的矿物，进而储存在基质中，或用于自我增殖。基质通常有导电性，因而个体间能通过电信号协调。"),
                detailBox.text("21世纪末看似意外的化石燃料产业复兴，本质上是由“匍匐集群”的技术革新所推动。在 Wotspire 公司发现某一型号的纳米机器人消化原油产生的二茂铁在基质中能够支持集群活动后，这些含铁的“深层石化产品”即藉由一些不见得光彩的手段突入市场。他们的生意一直延续到那场丑闻发生。"),
                detailBox.text("“蔓生集群”").setQuote(0xFFC1E52F),
                detailBox.image("minecraft:textures/item/apple.png").setBackgroundColor(0xFF585966),
                detailBox.text("称为“蔓生集群”的衍生形态部分算是自然造物。人类向炎热的地壳深处高歌猛进时，一些“匍匐集群”的基质成分开始自发形成链状高聚物，严重降低集群的机动性。由于此现象一般发生在相比设计工作温度更高的温度区间，没有任何纳米机器人企业愿意赔偿矿业集团因此产生的损失。这无可厚非。"),
                detailBox.text("不过，受此启发，Krakeshavna 公司首先于2098年调整了分子装配子集群的设计，使纳米机器人制造的基质成分形成更有序的高聚物纤维。这些纤维不仅不再阻碍集群移动，还提升了其物理搬运能力，使集群可破碎并卷挟矿物晶体返回。这对开采锆石等地壳深层的化学稳定矿物尤为有效。"),
                detailBox.text("“悬浮集群”").setQuote(0xFFC1E52F),
                detailBox.image("minecraft:textures/item/apple.png").setBackgroundColor(0xFF585966),
                detailBox.text("“悬浮集群”的形态设计早在纳米机器人技术探索的最初阶段就已提出，但因“匍匐集群”的成功而一度淡出视线。直至人类终于接近莫霍面，采取“悬浮集群”的产品才真正商业化。"),
                detailBox.text("不像其他集群形态，“悬浮集群”外观上类似早年科幻作品中的纳米机器人“蜂群”，这可能正是此设计被构思出来的原因。但谁要把科幻小说当成技术手册，那就昏了头了。"),
                detailBox.text("地壳上层的氧化性气氛会显著缩减悬浮个体的寿命；同时，公众对活性纳米粉尘的健康影响尤为关切。各种因素最终让“悬浮集群”的应用场合限定在地壳下层，正好填补因基质有机物热解隐患而难以正常工作的“匍匐集群”及“蔓生集群”的空缺。"),
                detailBox.text("由于不存在基质，“悬浮集群”的个体间协调通常依赖电磁感应。在下层地壳洞穴的热风吹拂下，移动的静电场发生子集群有时可制造危险的静电风暴。"),
                detailBox.text(Alignment.RIGHT, "&#9294A3By Lyuuke - CC-BY 4.0")
        );

        this.categoryBox = new CategoryBox(this, detailBox);
    }

    @Override
    public void addUIElements() {
        add(detailBox);
        add(categoryBox);
        add(detailBox.scrollBar);
        add(categoryBox.scrollBar);
    }
}
