import React from 'react';
import { Popover, Segmented, theme, Typography, Tooltip, Button } from 'antd';
import { SettingOutlined, CheckOutlined } from '@ant-design/icons';
import { useAppStore } from '../store/useAppStore';
import {
  COLOR_THEMES,
  FONT_FAMILIES,
  FONT_SIZES,
  UI_STYLES,
  type FontSize,
} from '../config/themeConfig';

const { Text } = Typography;

function SectionLabel({ children }: { children: React.ReactNode }) {
  const { token } = theme.useToken();
  return (
    <Text style={{
      fontSize: 11,
      fontWeight: 600,
      color: token.colorTextTertiary,
      letterSpacing: '0.06em',
    }}>
      {children}
    </Text>
  );
}

function ColorSwatch({
  color,
  label,
  active,
  onClick,
}: {
  color: string;
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  const { token } = theme.useToken();
  return (
    <div
      onClick={onClick}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        cursor: 'pointer',
      }}
    >
      <div
        style={{
          width: 26,
          height: 26,
          borderRadius: '50%',
          background: color,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          outline: active ? `2px solid ${color}` : '2px solid transparent',
          outlineOffset: 2,
          transition: 'outline-color 0.2s ease',
        }}
      >
        {active && <CheckOutlined style={{ color: '#fff', fontSize: 10, fontWeight: 700 }} />}
      </div>
      <Text style={{
        fontSize: 10,
        color: active ? token.colorText : token.colorTextTertiary,
        fontWeight: active ? 600 : 400,
        transition: 'color 0.15s ease',
      }}>
        {label}
      </Text>
    </div>
  );
}

function FontOption({
  label,
  fontValue,
  active,
  onClick,
}: {
  label: string;
  fontValue: string;
  active: boolean;
  onClick: () => void;
}) {
  const { token } = theme.useToken();
  return (
    <div
      onClick={onClick}
      style={{
        padding: '6px 10px',
        borderRadius: 8,
        cursor: 'pointer',
        border: active ? `1.5px solid ${token.colorPrimary}` : `1px solid ${token.colorBorderSecondary}`,
        background: active ? `${token.colorPrimary}0A` : 'transparent',
        transition: 'all 0.15s ease',
        textAlign: 'center',
      }}
    >
      <Text style={{
        fontSize: 13,
        fontFamily: fontValue,
        color: active ? token.colorPrimary : token.colorText,
        fontWeight: active ? 600 : 400,
      }}>
        {label}
      </Text>
    </div>
  );
}

function StyleOption({
  label,
  description,
  active,
  onClick,
}: {
  label: string;
  description: string;
  active: boolean;
  onClick: () => void;
}) {
  const { token } = theme.useToken();
  return (
    <div
      onClick={onClick}
      style={{
        flex: 1,
        padding: '8px 6px',
        borderRadius: 10,
        cursor: 'pointer',
        border: active ? `1.5px solid ${token.colorPrimary}` : `1px solid ${token.colorBorderSecondary}`,
        background: active ? `${token.colorPrimary}0A` : 'transparent',
        transition: 'all 0.15s ease',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 2,
      }}
    >
      <Text style={{
        fontSize: 13,
        fontWeight: active ? 600 : 500,
        color: active ? token.colorPrimary : token.colorText,
      }}>
        {label}
      </Text>
      <Text style={{
        fontSize: 10,
        color: token.colorTextTertiary,
      }}>
        {description}
      </Text>
    </div>
  );
}

function PopoverContent() {
  const {
    themeMode, toggleTheme,
    colorTheme, setColorTheme,
    fontFamily, setFontFamily,
    fontSize, setFontSize,
    uiStyle, setUIStyle,
  } = useAppStore();

  const normalFonts = Object.values(FONT_FAMILIES).filter(f => f.category === 'normal');
  const artisticFonts = Object.values(FONT_FAMILIES).filter(f => f.category === 'artistic');

  return (
    <div style={{ width: 280, display: 'flex', flexDirection: 'column', gap: 16, maxHeight: '70vh', overflowY: 'auto' }}>
      {/* 外观模式 */}
      <div>
        <SectionLabel>外观模式</SectionLabel>
        <div style={{ marginTop: 8 }}>
          <Segmented
            block
            value={themeMode}
            onChange={(val) => { if (val !== themeMode) toggleTheme(); }}
            options={[
              { label: '浅色', value: 'light' },
              { label: '深色', value: 'dark' },
            ]}
          />
        </div>
      </div>

      {/* 主题色 */}
      <div>
        <SectionLabel>主题色</SectionLabel>
        <div style={{ marginTop: 8, display: 'flex', gap: 12, justifyContent: 'flex-start', flexWrap: 'wrap', padding: '4px 4px' }}>
          {Object.values(COLOR_THEMES).map((ct) => (
            <ColorSwatch
              key={ct.key}
              color={ct.primary}
              label={ct.label}
              active={colorTheme === ct.key}
              onClick={() => setColorTheme(ct.key)}
            />
          ))}
        </div>
      </div>

      {/* UI 风格 */}
      <div>
        <SectionLabel>界面风格</SectionLabel>
        <div style={{ marginTop: 8, display: 'flex', gap: 8 }}>
          {Object.values(UI_STYLES).map((s) => (
            <StyleOption
              key={s.key}
              label={s.label}
              description={s.description}
              active={uiStyle === s.key}
              onClick={() => setUIStyle(s.key)}
            />
          ))}
        </div>
      </div>

      {/* 字体 - 常规 */}
      <div>
        <SectionLabel>字体</SectionLabel>
        <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 6 }}>
          {normalFonts.map((f) => (
            <FontOption
              key={f.key}
              label={f.label}
              fontValue={f.value}
              active={fontFamily === f.key}
              onClick={() => setFontFamily(f.key)}
            />
          ))}
        </div>
      </div>

      {/* 字体 - 艺术 */}
      <div>
        <SectionLabel>艺术字体</SectionLabel>
        <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 6 }}>
          {artisticFonts.map((f) => (
            <FontOption
              key={f.key}
              label={f.label}
              fontValue={f.value}
              active={fontFamily === f.key}
              onClick={() => setFontFamily(f.key)}
            />
          ))}
        </div>
      </div>

      {/* 字号 */}
      <div>
        <SectionLabel>字号</SectionLabel>
        <div style={{ marginTop: 8 }}>
          <Segmented
            block
            value={fontSize}
            onChange={(val) => setFontSize(val as FontSize)}
            options={Object.values(FONT_SIZES).map((s) => ({
              label: s.label,
              value: s.key,
            }))}
          />
        </div>
      </div>
    </div>
  );
}

export default function ThemePopover() {
  const { token } = theme.useToken();

  return (
    <Popover
      content={<PopoverContent />}
      title={
        <Text style={{ fontSize: 13, fontWeight: 600 }}>个性化设置</Text>
      }
      trigger="click"
      placement="bottomRight"
      arrow={{ pointAtCenter: true }}
      overlayInnerStyle={{ padding: '14px 18px 18px' }}
    >
      <Tooltip title="个性化设置" mouseEnterDelay={0.5}>
        <Button
          type="text"
          style={{
            width: 36,
            height: 36,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: token.colorTextSecondary,
            borderRadius: 8,
          }}
          icon={<SettingOutlined style={{ fontSize: 16 }} />}
        />
      </Tooltip>
    </Popover>
  );
}
