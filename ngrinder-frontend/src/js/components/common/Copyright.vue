<template>
    <div class="copyright-container">
        <div class="credit">
            <a href="https://naver.github.io/ngrinder" target="_blank" v-text="`nGrinder v${version}`"></a>
        </div>
    </div>
</template>

<script>
    import Component from 'vue-class-component';

    @Component({
        name: 'copyright',
        props: {
            version: {
                type: String,
                required: true,
            },
        },
    })
    export default class Copyright {}
</script>

<style lang="less" scoped>
    .copyright-container {
        font-size: 12px;
        position: fixed;
        height: 40px;
        left: 0;
        bottom: 0;
        width: 100%;
        background-color: #f5f4f2;

        a:hover {
            text-decoration: none;
        }

        .credit {
            height: 100%;
            margin: 0;
            display: flex;
            align-items: center;
            justify-content: center;
        }
    }
</style>
