package org.red5.io.webm;

import java.io.UnsupportedEncodingException;

import org.red5.io.matroska.ConverterException;
import org.red5.io.matroska.dtd.CompoundTag;
import org.red5.io.matroska.dtd.StringTag;
import org.red5.io.matroska.dtd.TagFactory;
import org.red5.io.matroska.dtd.UnsignedIntegerTag;

public class EBMLHeaderBuilder {
    public static CompoundTag build() throws ConverterException, UnsupportedEncodingException {
        return TagFactory.<CompoundTag> create("EBML").add(TagFactory.<UnsignedIntegerTag> create("EBMLVersion").setValue(1)).add(TagFactory.<UnsignedIntegerTag> create("EBMLReadVersion").setValue(1)).add(TagFactory.<UnsignedIntegerTag> create("EBMLMaxIDLength").setValue(4)).add(TagFactory.<UnsignedIntegerTag> create("EBMLMaxSizeLength").setValue(8)).add(TagFactory.<StringTag> create("DocType").setValue("webm"))
                .add(TagFactory.<UnsignedIntegerTag> create("DocTypeVersion").setValue(3)).add(TagFactory.<UnsignedIntegerTag> create("DocTypeReadVersion").setValue(2));

    }
}
