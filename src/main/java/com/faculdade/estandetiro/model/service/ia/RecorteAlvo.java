package com.faculdade.estandetiro.model.service.ia;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Recorta o personagem gerado pela IA: tira o fundo liso (magenta), corta nas bordas do personagem, reduz o
 * tamanho e põe um contorno claro com borda escura, como um recorte de papelão pintado.
 * Devolve um PNG com transparência.
 */
public final class RecorteAlvo {

    public static final int ALTURA_FINAL = 480;
    /** Distância de cor (RGB) até a qual o pixel é fundo puro. */
    static final double FUNDO_PURO = 70;
    /** Distância de cor a partir da qual o pixel é do personagem (entre as duas: borda suave). */
    static final double PERSONAGEM = 140;
    static final int CONTORNO_CLARO = 4;
    static final int CONTORNO_ESCURO = 6;
    private static final int COR_CLARA = 0xF2EAD8;
    private static final int COR_ESCURA = 0x2A1C12;

    private RecorteAlvo() {
    }

    public static byte[] processar(byte[] original) throws IOException {
        BufferedImage lida = ImageIO.read(new ByteArrayInputStream(original));
        if (lida == null) {
            throw new IOException("formato de imagem não suportado");
        }
        BufferedImage semFundo = removerFundo(lida);
        int[] caixa = caixaDoPersonagem(semFundo);
        BufferedImage recortada = semFundo.getSubimage(caixa[0], caixa[1],
                caixa[2] - caixa[0] + 1, caixa[3] - caixa[1] + 1);
        BufferedImage pronta = contornar(reduzir(recortada, ALTURA_FINAL));
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(pronta, "png", saida);
        return saida.toByteArray();
    }

    /** Transparência pela distância à cor do fundo, só na parte do fundo ligada às bordas da imagem. */
    static BufferedImage removerFundo(BufferedImage imagem) throws IOException {
        int w = imagem.getWidth(), h = imagem.getHeight();
        int[] px = imagem.getRGB(0, 0, w, h, null, 0, w);
        int fundo = corDoFundo(px, w, h);
        int fr = (fundo >> 16) & 255, fg = (fundo >> 8) & 255, fb = fundo & 255;

        float[] dist = new float[px.length];
        for (int i = 0; i < px.length; i++) {
            int r = (px[i] >> 16) & 255, g = (px[i] >> 8) & 255, b = px[i] & 255;
            dist[i] = (float) Math.sqrt((r - fr) * (r - fr) + (g - fg) * (g - fg) + (b - fb) * (b - fb));
        }
        if (fracaoDaBordaLisa(dist, w, h) < 0.6) {
            throw new IOException("o fundo não estava liso o bastante para ser removido");
        }
        boolean[] ligado = fundoLigadoAsBordas(dist, w, h);
        // "bolsões" de fundo presos dentro do personagem (entre o braço e o corpo, sob o chapéu...): se a cor é
        // praticamente a do fundo, também saem
        boolean[] fundoTotal = new boolean[px.length];
        for (int i = 0; i < px.length; i++) {
            fundoTotal[i] = ligado[i] || dist[i] < FUNDO_PURO;
        }
        boolean[] pertoDoFundo = vizinhanca(fundoTotal, w, h, 3);
        boolean fundoMagenta = fr > 150 && fb > 150 && fg < 110;

        int[] alfa = new int[px.length];
        int[] cor = new int[px.length];
        for (int i = 0; i < px.length; i++) {
            int r = (px[i] >> 16) & 255, g = (px[i] >> 8) & 255, b = px[i] & 255;
            int a = 255;
            if (fundoTotal[i]) {
                double t = (dist[i] - FUNDO_PURO) / (PERSONAGEM - FUNDO_PURO);
                a = (int) Math.round(Math.max(0, Math.min(1, t)) * 255);
            }
            if (a > 0 && a < 255) {
                // "desmistura" a cor do fundo que ficou na borda suave
                double al = Math.max(a / 255.0, 0.3);
                r = limitar((r - (1 - al) * fr) / al);
                g = limitar((g - (1 - al) * fg) / al);
                b = limitar((b - (1 - al) * fb) / al);
            }
            if (fundoMagenta && a > 0 && pertoDoFundo[i]) {
                // a compressão JPEG espalha o magenta na beirada do personagem: neutraliza esse tom
                int excesso = Math.min(r, b) - g;
                if (excesso > 0) {
                    r -= excesso;
                    b -= excesso;
                }
            }
            alfa[i] = a;
            cor[i] = (r << 16) | (g << 8) | b;
        }
        int[] erodido = erodir(alfa, w, h);   // encolhe 1 px: o contorno cobre o que sobrou de franja
        int[] saida = new int[px.length];
        for (int i = 0; i < px.length; i++) {
            saida[i] = (erodido[i] << 24) | cor[i];
        }
        BufferedImage resultado = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        resultado.setRGB(0, 0, w, h, saida, 0, w);
        return resultado;
    }

    /** Caixa {x0, y0, x1, y1} das partes visíveis; falha se o recorte não fizer sentido. */
    static int[] caixaDoPersonagem(BufferedImage img) throws IOException {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        long visiveis = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) > 40) {
                    visiveis++;
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        double fracao = visiveis / (double) (w * h);
        if (fracao < 0.02) {
            throw new IOException("o personagem não foi encontrado na imagem");
        }
        if (fracao > 0.85) {
            throw new IOException("o fundo não estava liso o bastante para ser removido");
        }
        return new int[]{x0, y0, x1, y1};
    }

    /** Reduz para no máximo {@code alturaMax}, em passos, com cores pré-multiplicadas (sem franja escura). */
    static BufferedImage reduzir(BufferedImage img, int alturaMax) {
        BufferedImage atual = converter(img, BufferedImage.TYPE_INT_ARGB_PRE);
        while (atual.getHeight() > alturaMax) {
            int altura = Math.max(alturaMax, atual.getHeight() / 2);
            int largura = Math.max(1, (int) Math.round(atual.getWidth() * (altura / (double) atual.getHeight())));
            BufferedImage menor = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = menor.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(atual, 0, 0, largura, altura, null);
            g.dispose();
            atual = menor;
        }
        return converter(atual, BufferedImage.TYPE_INT_ARGB);
    }

    /** Contorno claro (papelão) com uma borda escura por fora, atrás do personagem. */
    static BufferedImage contornar(BufferedImage fig) {
        int m = CONTORNO_ESCURO + 2;
        int fw = fig.getWidth(), fh = fig.getHeight();
        int w = fw + 2 * m, h = fh + 2 * m;
        int[] fpx = fig.getRGB(0, 0, fw, fh, null, 0, fw);
        int[] alfa = new int[w * h];
        int[] cor = new int[w * h];
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < fw; x++) {
                int p = fpx[y * fw + x];
                int i = (y + m) * w + (x + m);
                alfa[i] = p >>> 24;
                cor[i] = p & 0xFFFFFF;
            }
        }
        int[] claro = dilatar(alfa, w, h, CONTORNO_CLARO);
        int[] escuro = dilatar(alfa, w, h, CONTORNO_ESCURO);
        int[] saida = new int[w * h];
        for (int i = 0; i < saida.length; i++) {
            int contorno = sobre(COR_CLARA, claro[i] / 255.0, COR_ESCURA, escuro[i] / 255.0 * 0.8);
            saida[i] = sobre(cor[i], alfa[i] / 255.0, contorno & 0xFFFFFF, (contorno >>> 24) / 255.0);
        }
        BufferedImage resultado = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        resultado.setRGB(0, 0, w, h, saida, 0, w);
        return resultado;
    }

    // ------------------------------------------------------------------ auxiliares

    private static int corDoFundo(int[] px, int w, int h) {
        int n = 2 * w + 2 * h;
        int[] r = new int[n], g = new int[n], b = new int[n];
        int k = 0;
        for (int x = 0; x < w; x++) {
            for (int p : new int[]{px[x], px[(h - 1) * w + x]}) {
                r[k] = (p >> 16) & 255; g[k] = (p >> 8) & 255; b[k] = p & 255; k++;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int p : new int[]{px[y * w], px[y * w + w - 1]}) {
                r[k] = (p >> 16) & 255; g[k] = (p >> 8) & 255; b[k] = p & 255; k++;
            }
        }
        Arrays.sort(r);
        Arrays.sort(g);
        Arrays.sort(b);
        return (r[n / 2] << 16) | (g[n / 2] << 8) | b[n / 2];
    }

    /** Fração dos pixels da borda que têm a cor do fundo (o personagem pode encostar um pouco na borda). */
    private static double fracaoDaBordaLisa(float[] dist, int w, int h) {
        int lisos = 0, total = 0;
        for (int x = 0; x < w; x++) {
            lisos += (dist[x] < FUNDO_PURO ? 1 : 0) + (dist[(h - 1) * w + x] < FUNDO_PURO ? 1 : 0);
            total += 2;
        }
        for (int y = 0; y < h; y++) {
            lisos += (dist[y * w] < FUNDO_PURO ? 1 : 0) + (dist[y * w + w - 1] < FUNDO_PURO ? 1 : 0);
            total += 2;
        }
        return lisos / (double) total;
    }

    private static boolean[] fundoLigadoAsBordas(float[] dist, int w, int h) {
        boolean[] marcado = new boolean[w * h];
        int[] fila = new int[w * h];
        int ini = 0, fim = 0;
        for (int x = 0; x < w; x++) {
            fim = semear(x, dist, marcado, fila, fim);
            fim = semear((h - 1) * w + x, dist, marcado, fila, fim);
        }
        for (int y = 1; y < h - 1; y++) {
            fim = semear(y * w, dist, marcado, fila, fim);
            fim = semear(y * w + w - 1, dist, marcado, fila, fim);
        }
        while (ini < fim) {
            int i = fila[ini++];
            int x = i % w;
            if (x > 0) fim = semear(i - 1, dist, marcado, fila, fim);
            if (x < w - 1) fim = semear(i + 1, dist, marcado, fila, fim);
            if (i >= w) fim = semear(i - w, dist, marcado, fila, fim);
            if (i + w < w * h) fim = semear(i + w, dist, marcado, fila, fim);
        }
        return marcado;
    }

    private static int semear(int i, float[] dist, boolean[] marcado, int[] fila, int fim) {
        if (!marcado[i] && dist[i] < PERSONAGEM) {
            marcado[i] = true;
            fila[fim++] = i;
        }
        return fim;
    }

    /** Pixels a até {@code raio} de algum pixel marcado. */
    private static boolean[] vizinhanca(boolean[] marcado, int w, int h, int raio) {
        boolean[] saida = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!marcado[y * w + x]) continue;
                for (int dy = -raio; dy <= raio; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= h) continue;
                    for (int dx = -raio; dx <= raio; dx++) {
                        int xx = x + dx;
                        if (xx >= 0 && xx < w) saida[yy * w + xx] = true;
                    }
                }
            }
        }
        return saida;
    }

    private static int[] erodir(int[] alfa, int w, int h) {
        int[] saida = new int[alfa.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int min = 255;
                for (int dy = -1; dy <= 1 && min > 0; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int xx = x + dx, yy = y + dy;
                        int v = (xx < 0 || yy < 0 || xx >= w || yy >= h) ? 0 : alfa[yy * w + xx];
                        if (v < min) min = v;
                    }
                }
                saida[y * w + x] = min;
            }
        }
        return saida;
    }

    static int[] dilatar(int[] alfa, int w, int h, int raio) {
        List<int[]> disco = new ArrayList<>();
        for (int dy = -raio; dy <= raio; dy++) {
            for (int dx = -raio; dx <= raio; dx++) {
                if (dx * dx + dy * dy <= raio * raio + raio) disco.add(new int[]{dx, dy});
            }
        }
        int[] saida = new int[alfa.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int max = 0;
                for (int[] d : disco) {
                    int xx = x + d[0], yy = y + d[1];
                    if (xx < 0 || yy < 0 || xx >= w || yy >= h) continue;
                    int v = alfa[yy * w + xx];
                    if (v > max) {
                        max = v;
                        if (max == 255) break;
                    }
                }
                saida[y * w + x] = max;
            }
        }
        return saida;
    }

    /** Cor A (com alfa) por cima da cor B (com alfa); devolve ARGB não pré-multiplicado. */
    private static int sobre(int corA, double alfaA, int corB, double alfaB) {
        double a = alfaA + alfaB * (1 - alfaA);
        if (a <= 0.001) return 0;
        int r = limitar((((corA >> 16) & 255) * alfaA + ((corB >> 16) & 255) * alfaB * (1 - alfaA)) / a);
        int g = limitar((((corA >> 8) & 255) * alfaA + ((corB >> 8) & 255) * alfaB * (1 - alfaA)) / a);
        int b = limitar(((corA & 255) * alfaA + (corB & 255) * alfaB * (1 - alfaA)) / a);
        return (limitar(a * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    private static BufferedImage converter(BufferedImage img, int tipo) {
        BufferedImage saida = new BufferedImage(img.getWidth(), img.getHeight(), tipo);
        Graphics2D g = saida.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return saida;
    }

    private static int limitar(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
