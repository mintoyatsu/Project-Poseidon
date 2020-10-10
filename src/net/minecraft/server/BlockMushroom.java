package net.minecraft.server;

import org.bukkit.event.block.BlockSpreadEvent;

import java.util.Random;

public class BlockMushroom extends BlockFlower {

    protected BlockMushroom(int i, int j) {
        super(i, j);
        float f = 0.2F;

        this.a(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 2.0F, 0.5F + f);
        this.a(true);
    }

    public void a(World world, int i, int j, int k, Random random) {
        if (random.nextInt(100) == 0) {
            org.bukkit.World bworld = world.getWorld();
            byte shroomRadius = 4;
            int maxShrooms = 5;

            for (int x = i - shroomRadius; x <= i + shroomRadius; x++) {
                for (int z = k - shroomRadius; z <= k + shroomRadius; z++) {
                    for (int y = j - 1; y <= j + 1; y++) {
                        if (bworld.getBlockAt(x, y, z).getTypeId() == this.id && --maxShrooms <= 0) {
                            return;
                        }
                    }
                }
            }

            int l = i + random.nextInt(3) - 1;
            int i1 = j + random.nextInt(2) - random.nextInt(2);
            int j1 = k + random.nextInt(3) - 1;

            for (int l1 = 0; l1 < 4; l1++) {
                if (world.isEmpty(l, i1, j1) && this.f(world, l, i1, j1)) {
                    i = l;
                    j = i1;
                    k = j1;
                }

                l = i + random.nextInt(3) - 1;
                i1 = j + random.nextInt(2) - random.nextInt(2);
                j1 = k + random.nextInt(3) - 1;
            }

            if (world.isEmpty(l, i1, j1) && this.f(world, l, i1, j1)) {
                // CraftBukkit start
                org.bukkit.block.BlockState blockState = bworld.getBlockAt(l, i1, j1).getState();
                blockState.setTypeId(this.id);

                BlockSpreadEvent event = new BlockSpreadEvent(blockState.getBlock(), bworld.getBlockAt(i, j, k), blockState);
                world.getServer().getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    blockState.update(true);
                }
                // CraftBukkit end
            }
        }
    }

    protected boolean c(int i) {
        return Block.o[i];
    }

    public boolean f(World world, int i, int j, int k) {
        return j >= 0 && j < 128 ? world.k(i, j, k) < 13 && this.c(world.getTypeId(i, j - 1, k)) : false;
    }
}
