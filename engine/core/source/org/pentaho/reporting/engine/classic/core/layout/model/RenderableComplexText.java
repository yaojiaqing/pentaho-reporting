package org.pentaho.reporting.engine.classic.core.layout.model;

import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.BreakIterator;

import org.pentaho.reporting.engine.classic.core.ReportAttributeMap;
import org.pentaho.reporting.engine.classic.core.layout.model.context.NodeLayoutProperties;
import org.pentaho.reporting.engine.classic.core.layout.output.OutputProcessorFeature;
import org.pentaho.reporting.engine.classic.core.layout.output.OutputProcessorMetaData;
import org.pentaho.reporting.engine.classic.core.layout.output.RenderUtility;
import org.pentaho.reporting.engine.classic.core.layout.process.util.RichTextSpec;
import org.pentaho.reporting.engine.classic.core.layout.process.util.TextHelper;
import org.pentaho.reporting.engine.classic.core.metadata.ElementType;
import org.pentaho.reporting.engine.classic.core.style.StyleSheet;
import org.pentaho.reporting.engine.classic.core.style.TextStyleKeys;
import org.pentaho.reporting.engine.classic.core.util.InstanceID;
import org.pentaho.reporting.engine.classic.core.util.geom.StrictGeomUtility;

public class RenderableComplexText extends RenderNode
{
  private RichTextSpec richText;
  private int start;
  private int end;
  private String text;
  private TextLayout textLayout;
  private boolean forceLinebreak;

  public RenderableComplexText(final StyleSheet styleSheet,
                               final InstanceID instanceID,
                               final ElementType elementType,
                               final ReportAttributeMap<Object> attributes,
                               final RichTextSpec text,
                               final int start,
                               final int end)
  {
    super(new NodeLayoutProperties(styleSheet, attributes, instanceID, elementType));
    if (start >= end)
    {
      throw new IllegalArgumentException();
    }

    this.text = text.getText().substring(start, end);
    this.richText = text;
    this.forceLinebreak = false;
    this.start = start;
    this.end = end;
  }

  public RenderableComplexText(final StyleSheet styleSheet,
                               final InstanceID instanceID,
                               final ElementType elementType,
                               final ReportAttributeMap<Object> attributes,
                               final String text)
  {
    super(new NodeLayoutProperties(styleSheet, attributes, instanceID, elementType));
    this.text = text;
    this.richText = null;
    this.forceLinebreak = false;
    this.start = 0;
    this.end = text.length();
  }


  public void computeMinimumChunkWidth(final OutputProcessorMetaData data)
  {
    if (getMinimumChunkWidth() != 0)
    {
      return;
    }

    if (data.isFeatureSupported(OutputProcessorFeature.STRICT_COMPATIBILITY) == false &&
         getStyleSheet().getBooleanStyleProperty(TextStyleKeys.WORDBREAK) == false)
    {
      return;
    }

    long minimumChunkWidth = 0;
    BreakIterator wordInstance = BreakIterator.getWordInstance();
    wordInstance.setText(text);

    final boolean antiAliasing = RenderUtility.isFontSmooth(getStyleSheet(), data);
    final FontRenderContext fontRenderContext = new FontRenderContext(null, antiAliasing, true);

    int start = wordInstance.first();
    for (int end = wordInstance.next(); end != BreakIterator.DONE; start = end, end = wordInstance.next())
    {
      String word = text.substring(start, end);
      AttributedCharacterIterator attributedCharacterIterator =
          new TextHelper().computeText(this, word).createAttributedCharacterIterator();
      TextLayout t = new TextLayout(attributedCharacterIterator, fontRenderContext);
      double width = t.getVisibleAdvance();
      final long wordMinChunkWidth = StrictGeomUtility.toInternalValue(width);
      minimumChunkWidth = Math.max(minimumChunkWidth, wordMinChunkWidth);
    }

    setMinimumChunkWidth(minimumChunkWidth);
  }

  public int getNodeType()
  {
    return LayoutNodeTypes.TYPE_NODE_COMPLEX_TEXT;
  }

  public String getRawText()
  {
    return text;
  }

  public RichTextSpec getRichText()
  {
    if (richText == null)
    {
      // code-smell - we probably should introduce a "ProcessedComplexText" type to distinguish between
      // raw text and text that has been processed by the CanvasMinor-step.
      throw new IllegalStateException("Calling 'getRichText' is only valid after layouting is complete.");
    }
    return richText.substring(start, end);
  }

  public TextLayout getTextLayout()
  {
    return textLayout;
  }

  public void setTextLayout(final TextLayout textLayout)
  {
    this.textLayout = textLayout;
  }

  public void setForceLinebreak(final boolean forceLinebreak)
  {
    this.forceLinebreak = forceLinebreak;
  }

  public boolean isForceLinebreak()
  {
    return forceLinebreak;
  }

  public RenderableComplexText merge(final RenderableComplexText suffix)
  {
    if (richText != suffix.richText)
    {
      throw new IllegalStateException("Not from the same source");
    }

    final RenderableComplexText text = (RenderableComplexText) derive(true);
    text.end = suffix.end;
    text.setMinimumChunkWidth(Math.max (getMinimumChunkWidth(), suffix.getMinimumChunkWidth()));
    return text;
  }

  public boolean isSameSource(final RenderableComplexText suffix)
  {
    if (richText != suffix.richText)
    {
      return false;
    }
    return true;
  }
}
