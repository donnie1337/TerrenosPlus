# TerrenosPlus

Sistema próprio de terrenos para Paper 26.3, inspirado na experiência de uso do GriefPrevention.

## Primeira versão

- Seleção de dois cantos com pá de ouro.
- Terrenos retangulares 2D que protegem toda a coluna vertical.
- Detecção de sobreposição.
- Limite configurável de quantidade e área.
- Proteção contra quebrar/colocar blocos, interações, baldes, explosões, pistões e fluidos.
- Persistência em `plugins/TerrenosPlus/terrenos.yml`.
- Índice por chunk para consulta rápida.
- API própria via Bukkit ServicesManager para integrações como ScorePlus.
- `/terreno info`, `/terreno remover` e `/terreno listar`.

## Uso

1. Segure uma pá de ouro.
2. Clique com o botão direito no primeiro canto.
3. Clique com o botão direito no canto oposto.
4. O terreno é criado se não houver sobreposição e os limites forem válidos.

A implementação é própria. O GriefPrevention é usado apenas como referência conceitual de UX para claims.
