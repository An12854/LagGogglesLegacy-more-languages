package com.falsepattern.laggoggles.util;

import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;

public class ClickableLink {

    public static TextComponentString getLink(String link){
        TextComponentString text = new TextComponentString(TextFormatting.BLUE + link);
        Style style = text.getStyle();
        style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
        return text;
    }
}
