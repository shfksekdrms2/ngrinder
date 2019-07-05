<template>
    <div class="intro-button-container clickable" rel="popover"
         :title="title" data-html="ture" data-placement="left"
         :data-content="i18n('intro.public.button.show')" @click="startIntroJs">
        <code class="intro-button-title" v-text="title"></code>
    </div>
</template>

<script>
    import Component from 'vue-class-component';
    import intro from 'intro.js';
    import Base from '../Base.vue';

    @Component({
        name: 'introButton',
        props: {
            title: {
                type: String,
                default: 'Tip',
            },
        },
    })
    export default class IntroButton extends Base {
        startIntroJs() {
            intro.introJs().start();
        }
    }
</script>

<style lang="less" scoped>
    .intro-button-container {
        text-align: right;

        code {
            border-radius: 2px;
            font-size: 12px;
            padding: 2px 4px;
            color: #d14;
            background-color: #f7f7f9;
            border: 1px solid #e1e1e8;
            white-space: nowrap;
        }

        .intro-button-title {
            margin-right: -35px;
        }
    }
</style>
