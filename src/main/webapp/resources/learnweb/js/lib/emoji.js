/**
 * Original sources taken from https://github.com/dshemendiuk/native-emoji-picker
 * Modified by Oleh Astappiev
 */

class Emoji {
  constructor() {
    // I don't know what is better: keeping it in memory or creating by each request
    this.emojiData = [
      {
        name: 'faces',
        items: [
          0x1F600, 0x1F601, 0x1F602, 0x1F923, 0x1F603, 0x1F604, 0x1F605, 0x1F606,
          0x1F609, 0x1F60A, 0x1F60B, 0x1F60E, 0x1F60D, 0x1F618, 0x1F970, 0x1F617,
          0x1F619, 0x1F61A, 0x1F642, 0x1F917, 0x1F929, 0x1F914, 0x1F928, 0x1F610,
          0x1F611, 0x1F636, 0x1F644, 0x1F60F, 0x1F623, 0x1F625, 0x1F62E, 0x1F910,
          0x1F62F, 0x1F62A, 0x1F62B, 0x1F634, 0x1F60C, 0x1F61B, 0x1F61C, 0x1F61D,
          0x1F924, 0x1F612, 0x1F613, 0x1F614, 0x1F615, 0x1F643, 0x1F911, 0x1F632,
          0x1F641, 0x1F616, 0x1F61E, 0x1F61F, 0x1F624, 0x1F622, 0x1F62D, 0x1F626,
          0x1F627, 0x1F628, 0x1F629, 0x1F92F, 0x1F62C, 0x1F630, 0x1F631, 0x1F975,
          0x1F976, 0x1F633, 0x1F92A, 0x1F635, 0x1F621, 0x1F620, 0x1F92C, 0x1F637,
          0x1F912, 0x1F915, 0x1F922, 0x1F92E, 0x1F927, 0x1F607, 0x1F920, 0x1F921,
          0x1F973, 0x1F974, 0x1F97A, 0x1F925, 0x1F92B, 0x1F92D, 0x1F9D0, 0x1F913,
          0x1F608, 0x1F47F, 0x1F479, 0x1F47A, 0x1F480, 0x1F47B, 0x1F47D, 0x1F916,
          0x1F4A9, 0x1F63A, 0x1F638, 0x1F639, 0x1F63B, 0x1F63C, 0x1F63D, 0x1F640,
          0x1F63F, 0x1F63E, 0x1F467, 0x1F9D2, 0x1F466, 0x1F469, 0x1F9D1, 0x1F468,
          0x1F475, 0x1F9D3, 0x1F474, 0x1F472, 0x1F9D5, 0x1F9D4, 0x1F470, 0x1F935,
          0x1F478, 0x1F934, 0x1F936, 0x1F385, 0x1F47C, 0x1F930, 0x1F931, 0x1F485,
          0x1F933, 0x1F483, 0x1F57A, 0x1F574, 0x1F46B, 0x1F46D, 0x1F46C, 0x1F491,
          0x1F48F, 0x1F46A, 0x1F932, 0x1F91D, 0x1F44D, 0x1F450, 0x1F64C, 0x1F44F,
          0x1F44E, 0x1F44A, 0x270A, 0x1F91B, 0x1F91C, 0x1F91E, 0x1F91F, 0x1F918,
          0x1F44C, 0x1F448, 0x1F449, 0x1F446, 0x1F447, 0x270B, 0x1F91A, 0x1F590,
          0x1F596, 0x1F44B, 0x1F919, 0x1F4AA, 0x1F9B5, 0x1F9B6, 0x1F595, 0x1F64F,
          0x1F48D, 0x1F484, 0x1F48B, 0x1F444, 0x1F445, 0x1F442, 0x1F443, 0x1F463,
          0x1F441, 0x1F440, 0x1F9E0, 0x1F9B4, 0x1F9B7, 0x1F5E3, 0x1F464, 0x1F465,
        ],
        // eslint-disable-next-line max-len
        icon: '<svg id="faces" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 150 150"><path id="faces" d="M74.34,128.48a53.5,53.5,0,1,1,37.84-15.67,53.16,53.16,0,0,1-37.84,15.67Zm0-97.89a44.4,44.4,0,1,0,31.4,13,44.07,44.07,0,0,0-31.4-13Z"/><path id="faces" d="M74.35,108A33.07,33.07,0,0,1,41.29,75a2.28,2.28,0,0,1,2.27-2.28h0A2.27,2.27,0,0,1,45.83,75a28.52,28.52,0,0,0,57,0,2.27,2.27,0,0,1,4.54,0A33.09,33.09,0,0,1,74.35,108Z"/><path id="faces" d="M58.84,62a6.81,6.81,0,1,0,6.81,6.81A6.81,6.81,0,0,0,58.84,62Z"/><path id="faces" d="M89.87,62a6.81,6.81,0,1,0,6.81,6.81A6.82,6.82,0,0,0,89.87,62Z"/></svg>',
      },
      {
        name: 'nature',
        items: [
          0x1F436, 0x1F431, 0x1F42D, 0x1F439, 0x1F430, 0x1F98A, 0x1F99D, 0x1F43B,
          0x1F43C, 0x1F998, 0x1F9A1, 0x1F428, 0x1F42F, 0x1F981, 0x1F42E, 0x1F437,
          0x1F43D, 0x1F438, 0x1F435, 0x1F648, 0x1F649, 0x1F64A, 0x1F412, 0x1F414,
          0x1F427, 0x1F426, 0x1F424, 0x1F423, 0x1F425, 0x1F986, 0x1F9A2, 0x1F985,
          0x1F989, 0x1F99A, 0x1F99C, 0x1F987, 0x1F43A, 0x1F417, 0x1F434, 0x1F984,
          0x1F41D, 0x1F41B, 0x1F98B, 0x1F40C, 0x1F41A, 0x1F41E, 0x1F41C, 0x1F997,
          0x1F577, 0x1F578, 0x1F982, 0x1F99F, 0x1F9A0, 0x1F422, 0x1F40D, 0x1F98E,
          0x1F996, 0x1F995, 0x1F419, 0x1F991, 0x1F990, 0x1F980, 0x1F421, 0x1F420,
          0x1F41F, 0x1F42C, 0x1F433, 0x1F40B, 0x1F988, 0x1F40A, 0x1F405, 0x1F406,
          0x1F993, 0x1F98D, 0x1F418, 0x1F98F, 0x1F99B, 0x1F42A, 0x1F42B, 0x1F999,
          0x1F992, 0x1F403, 0x1F402, 0x1F404, 0x1F40E, 0x1F416, 0x1F40F, 0x1F411,
          0x1F410, 0x1F98C, 0x1F415, 0x1F429, 0x1F408, 0x1F413, 0x1F983, 0x1F54A,
          0x1F407, 0x1F401, 0x1F400, 0x1F43F, 0x1F994, 0x1F43E, 0x1F409, 0x1F432,
          0x1F335, 0x1F384, 0x1F332, 0x1F333, 0x1F334, 0x1F331, 0x1F33F, 0x1F340,
          0x1F38D, 0x1F38B, 0x1F343, 0x1F342, 0x1F341, 0x1F344, 0x1F33E, 0x1F490,
          0x1F337, 0x1F339, 0x1F940, 0x1F33A, 0x1F338, 0x1F33C, 0x1F33B, 0x1F31E,
          0x1F31D, 0x1F31B, 0x1F31C, 0x1F31A, 0x1F315, 0x1F316, 0x1F317, 0x1F318,
          0x1F311, 0x1F312, 0x1F313, 0x1F314, 0x1F319, 0x1F30E, 0x1F30D, 0x1F30F,
          0x1F4AB, 0x1F31F, 0x2728, 0x1F4A5, 0x1F525, 0x1F32A, 0x1F308, 0x1F324,
          0x1F325, 0x1F326, 0x1F327, 0x26C8, 0x1F329, 0x1F328, 0x1F32C, 0x1F4A8,
          0x1F4A7, 0x1F4A6, 0x1F30A, 0x1F32B,
        ],
        // eslint-disable-next-line max-len
        icon: '<svg id="nature" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 150 150"><path id="nature" d="M59.9,91.75h0c-22.46,0-41.82-19.34-44.09-44A52.1,52.1,0,0,1,16,36.8a4.51,4.51,0,0,1,2.63-3.62,39.79,39.79,0,0,1,12.74-3.37c23.92-2.15,45.35,17.83,47.74,43.86a52.77,52.77,0,0,1-.15,10.93,4.56,4.56,0,0,1-2.64,3.62,39.67,39.67,0,0,1-12.73,3.36c-1.23.11-2.45.17-3.66.17ZM24.76,40.49a41.29,41.29,0,0,0,.09,6.4C26.7,67,42.09,82.66,59.9,82.67h0c.94,0,1.88,0,2.83-.14a30.39,30.39,0,0,0,7.41-1.62,41.14,41.14,0,0,0-.11-6.4C68.09,53.38,51.11,37.08,32.17,38.86a30.78,30.78,0,0,0-7.41,1.63Z"/><path id="nature" d="M36.68,125.64a4.53,4.53,0,0,1-4.33-3.17,53.32,53.32,0,0,1-2.26-11A50.42,50.42,0,0,1,39.51,76.6c7.35-9.91,17.84-16,29.5-17,1.16-.11,2.33-.13,3.47-.13a4.54,4.54,0,0,1,4.33,3.16,51.59,51.59,0,0,1,2.27,11.08,50.39,50.39,0,0,1-9.42,34.8c-7.35,9.91-17.83,16-29.5,17a17.63,17.63,0,0,1-3.48.12ZM69.09,68.69A32.41,32.41,0,0,0,46.8,82a42.57,42.57,0,0,0-6.71,34.38,32.38,32.38,0,0,0,22.28-13.32A41.35,41.35,0,0,0,70,74.51a39.38,39.38,0,0,0-.94-5.82Z"/><path id="nature" d="M90.27,91.75c-1.22,0-2.43-.06-3.66-.17a39.67,39.67,0,0,1-12.73-3.36,4.57,4.57,0,0,1-2.64-3.61,53.38,53.38,0,0,1-.17-10.93c2.41-26,23.7-46.07,47.76-43.87a39.74,39.74,0,0,1,12.73,3.37,4.57,4.57,0,0,1,2.64,3.62,53.35,53.35,0,0,1,.16,10.92c-2.28,24.69-21.65,44-44.09,44ZM80,80.91a30.57,30.57,0,0,0,7.42,1.62c19.07,1.78,35.92-14.53,37.87-35.64a42.55,42.55,0,0,0,.1-6.4A30.86,30.86,0,0,0,118,38.86C99,37.07,82.06,53.38,80.12,74.51a43.91,43.91,0,0,0-.1,6.4Z"/><path id="nature" d="M113.49,125.64h0c-1.16,0-2.3,0-3.46-.12-23.9-2.21-41.36-25.47-38.94-51.85A53.52,53.52,0,0,1,73.34,62.6a4.55,4.55,0,0,1,4.33-3.16c1.16,0,2.34,0,3.51.13,11.64,1.07,22.11,7.12,29.48,17a50.51,50.51,0,0,1,9.42,34.81,53.51,53.51,0,0,1-2.26,11,4.54,4.54,0,0,1-4.33,3.19ZM81.08,68.69a42.53,42.53,0,0,0-1,5.82c-1.94,21.1,11.45,39.71,29.95,41.88A42.38,42.38,0,0,0,103.36,82,32.42,32.42,0,0,0,81.08,68.69Z"/><path id="nature" d="M75.08,45.45a7.83,7.83,0,1,0,7.83,7.83,7.83,7.83,0,0,0-7.83-7.83Z"/><path id="nature" d="M76.29,51.89a2.26,2.26,0,0,1-2.14-3A46,46,0,0,1,92.82,25.34a2.27,2.27,0,1,1,2.4,3.86A41.4,41.4,0,0,0,78.43,50.39a2.28,2.28,0,0,1-2.14,1.5Z"/><path id="nature" d="M73.87,51.89a2.28,2.28,0,0,1-2.14-1.5A41.35,41.35,0,0,0,54.94,29.2a2.27,2.27,0,0,1,2.39-3.86A46,46,0,0,1,76,48.85a2.28,2.28,0,0,1-1.37,2.91,2.31,2.31,0,0,1-.77.13Z"/></svg>',
      },
      {
        name: 'food',
        items: [
          0x1F34F, 0x1F34E, 0x1F350, 0x1F34A, 0x1F34B, 0x1F34C, 0x1F349, 0x1F347,
          0x1F353, 0x1F348, 0x1F352, 0x1F351, 0x1F34D, 0x1F96D, 0x1F965, 0x1F95D,
          0x1F345, 0x1F346, 0x1F951, 0x1F966, 0x1F952, 0x1F96C, 0x1F336, 0x1F33D,
          0x1F955, 0x1F954, 0x1F360, 0x1F950, 0x1F35E, 0x1F956, 0x1F968, 0x1F96F,
          0x1F9C0, 0x1F95A, 0x1F373, 0x1F95E, 0x1F953, 0x1F969, 0x1F357, 0x1F356,
          0x1F32D, 0x1F354, 0x1F35F, 0x1F355, 0x1F96A, 0x1F959, 0x1F32E, 0x1F32F,
          0x1F957, 0x1F958, 0x1F96B, 0x1F35D, 0x1F35C, 0x1F372, 0x1F35B, 0x1F363,
          0x1F371, 0x1F95F, 0x1F364, 0x1F359, 0x1F35A, 0x1F358, 0x1F365, 0x1F96E,
          0x1F960, 0x1F362, 0x1F361, 0x1F367, 0x1F368, 0x1F366, 0x1F967, 0x1F370,
          0x1F382, 0x1F36E, 0x1F36D, 0x1F36C, 0x1F36B, 0x1F37F, 0x1F9C2, 0x1F369,
          0x1F36A, 0x1F330, 0x1F95C, 0x1F36F, 0x1F95B, 0x1F37C, 0x1F375, 0x1F964,
          0x1F376, 0x1F37A, 0x1F37B, 0x1F942, 0x1F377, 0x1F943, 0x1F378, 0x1F379,
          0x1F37E, 0x1F944, 0x1F374, 0x1F37D, 0x1F963, 0x1F961, 0x1F962,
        ],
        // eslint-disable-next-line max-len
        icon: '<svg id="food" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 150 150"><path id="food" d="M104,20.76h.15c15.83.52,24.08,21.48,24.07,32.56.26,12.42-10.72,23.55-24,24.21a3.53,3.53,0,0,1-.46,0c-13.25-.66-24.23-11.8-24-24.3,0-11,8.26-31.95,24.07-32.47Zm0,47.69c8.25-.54,15.3-7.51,15.14-15,0-8.12-6.22-23.1-15.14-23.57-8.9.46-15.14,15.45-15.14,23.48-.14,7.61,6.9,14.59,15.14,15.13Z"/><path id="food" d="M97.19,69.21h.14a4.53,4.53,0,0,1,4.4,4.68l-1.48,46.92a1.59,1.59,0,0,0,.5,1.06,4.6,4.6,0,0,0,3.25,1.19h0a4.57,4.57,0,0,0,3.26-1.2,1.53,1.53,0,0,0,.49-1l-1.48-46.95a4.54,4.54,0,1,1,9.08-.28l1.47,46.91a10.42,10.42,0,0,1-3,7.65,13.65,13.65,0,0,1-9.81,4h0a13.58,13.58,0,0,1-9.79-4,10.42,10.42,0,0,1-3-7.67l1.48-46.89a4.53,4.53,0,0,1,4.53-4.4Z"/><path id="food" d="M41.84,69.21H42a4.53,4.53,0,0,1,4.4,4.68L44.9,120.81a1.57,1.57,0,0,0,.5,1.06,4.6,4.6,0,0,0,3.25,1.19h0a4.51,4.51,0,0,0,3.24-1.19,1.48,1.48,0,0,0,.5-1L50.93,73.89a4.53,4.53,0,0,1,4.39-4.68A4.4,4.4,0,0,1,60,73.61l1.48,46.91a10.49,10.49,0,0,1-3,7.66,13.57,13.57,0,0,1-9.78,4h0a13.59,13.59,0,0,1-9.78-4,10.48,10.48,0,0,1-3-7.67l1.48-46.9a4.54,4.54,0,0,1,4.54-4.4Z"/><path id="food" d="M28.59,20.76a4.54,4.54,0,0,1,4.54,4.54V51a15.52,15.52,0,0,0,31,0V25.3a4.55,4.55,0,0,1,9.09,0V51a24.61,24.61,0,1,1-49.21,0V25.3a4.54,4.54,0,0,1,4.54-4.54Z"/><path id="food" d="M55.34,20.76a4.54,4.54,0,0,1,4.54,4.54v19a4.54,4.54,0,1,1-9.08,0v-19a4.54,4.54,0,0,1,4.54-4.54Z"/><path id="food" d="M42,20.76a4.54,4.54,0,0,1,4.54,4.54v19a4.54,4.54,0,1,1-9.08,0v-19A4.54,4.54,0,0,1,42,20.76Z"/></svg>',
      },
      {
        name: 'activity',
        items: [
          0x1F9D7, 0x1F93A, 0x1F3C7, 0x26F7, 0x1F3C2, 0x1F3CC, 0x1F3C4, 0x1F6A3,
          0x1F3CA, 0x26F9, 0x1F3CB, 0x1F6B4, 0x1F6B5, 0x1F938, 0x1F93C, 0x1F93D,
          0x1F93E, 0x1F939, 0x1F9D8, 0x1F3AA, 0x1F6F9, 0x1F6F6, 0x1F397, 0x1F39F,
          0x1F3AB, 0x1F396, 0x1F3C6, 0x1F3C5, 0x1F947, 0x1F948, 0x1F949, 0x26BD,
          0x26BE, 0x1F94E, 0x1F3C0, 0x1F3D0, 0x1F3C8, 0x1F3C9, 0x1F3BE, 0x1F94F,
          0x1F3B3, 0x1F3CF, 0x1F3D1, 0x1F3D2, 0x1F94D, 0x1F3D3, 0x1F3F8, 0x1F94A,
          0x1F94B, 0x1F945, 0x26F3, 0x26F8, 0x1F3A3, 0x1F3BD, 0x1F3BF, 0x1F6F7,
          0x1F94C, 0x1F3AF, 0x1F3B1, 0x1F3AE, 0x1F3B0, 0x1F3B2, 0x1F9E9, 0x265F,
          0x1F3AD, 0x1F3A8, 0x1F9F5, 0x1F9F6, 0x1F3BC, 0x1F3A4, 0x1F3A7, 0x1F3B7,
          0x1F3B8, 0x1F3B9, 0x1F3BA, 0x1F3BB, 0x1F941, 0x1F3AC, 0x1F3F9,
        ],
        // eslint-disable-next-line max-len
        icon: '<svg id="activity" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 150 150"><path id="activity" d="M75.35,130.24a53.49,53.49,0,1,1,53.48-53.49,53.55,53.55,0,0,1-53.48,53.49Zm0-97.89a44.41,44.41,0,1,0,44.4,44.4,44.1,44.1,0,0,0-44.4-44.4Z"/><path id="activity" d="M119.24,84.08A51.29,51.29,0,0,1,68,32.86a49.44,49.44,0,0,1,.26-5,2.26,2.26,0,0,1,2-2c1.66-.16,3.34-.25,5-.25a51.26,51.26,0,0,1,51.21,51.21c0,1.71-.09,3.38-.25,5a2.28,2.28,0,0,1-2,2c-1.65.16-3.33.25-5,.25ZM72.64,30.16c-.06.9-.08,1.79-.08,2.7a46.73,46.73,0,0,0,46.68,46.68q1.37,0,2.7-.09c.06-.89.08-1.79.08-2.7A46.72,46.72,0,0,0,75.35,30.08c-.91,0-1.82,0-2.71.08Z"/><path id="activity" d="M75.35,128A51.28,51.28,0,0,1,24.12,76.76c0-1.7.1-3.38.25-5a2.29,2.29,0,0,1,2-2c1.66-.16,3.33-.25,5.05-.25a51.27,51.27,0,0,1,51.21,51.22c0,1.69-.09,3.37-.25,5a2.27,2.27,0,0,1-2,2c-1.66.16-3.32.25-5,.25ZM28.75,74.05c-.05.9-.09,1.8-.09,2.71a46.74,46.74,0,0,0,46.69,46.67c.91,0,1.8,0,2.7-.08,0-.9.08-1.8.08-2.7A46.73,46.73,0,0,0,31.46,74c-.91,0-1.81,0-2.71.08Z"/><polygon id="activity" points="42.69 112.61 39.48 109.4 108 40.88 111.21 44.1 42.69 112.61 42.69 112.61"/></svg>',
      },
      {
        name: 'transport',
        items: [
          0x1F697, 0x1F695, 0x1F699, 0x1F68C, 0x1F68E, 0x1F3CE, 0x1F693, 0x1F691,
          0x1F692, 0x1F690, 0x1F69A, 0x1F69B, 0x1F69C, 0x1F6F4, 0x1F6B2, 0x1F6F5,
          0x1F3CD, 0x1F6A8, 0x1F694, 0x1F68D, 0x1F698, 0x1F696, 0x1F6A1, 0x1F6A0,
          0x1F69F, 0x1F683, 0x1F68B, 0x1F69E, 0x1F69D, 0x1F684, 0x1F685, 0x1F688,
          0x1F682, 0x1F686, 0x1F687, 0x1F68A, 0x1F689, 0x1F6EB, 0x1F6EC, 0x1F6E9,
          0x1F4BA, 0x1F6F0, 0x1F680, 0x1F6F8, 0x1F681, 0x1F6F6, 0x1F6A4, 0x1F6E5,
          0x1F6F3, 0x26F4, 0x1F6A2, 0x1F6A7, 0x1F6A6, 0x1F6A5, 0x1F68F, 0x1F5FA,
          0x1F5FF, 0x1F5FD, 0x1F5FC, 0x1F3F0, 0x1F3EF, 0x1F3DF, 0x1F3A1, 0x1F3A2,
          0x1F3A0, 0x26F1, 0x1F3D6, 0x1F3DD, 0x1F3DC, 0x1F30B, 0x26F0, 0x1F3D4,
          0x1F5FB, 0x1F3D5, 0x1F3E0, 0x1F3E1, 0x1F3D8, 0x1F3DA, 0x1F3D7, 0x1F3ED,
          0x1F3E2, 0x1F3EC, 0x1F3E3, 0x1F3E4, 0x1F3E5, 0x1F3E6, 0x1F3E8, 0x1F3EA,
          0x1F3EB, 0x1F3E9, 0x1F492, 0x1F3DB, 0x1F54C, 0x1F54D, 0x1F54B, 0x26E9,
          0x1F6E4, 0x1F6E3, 0x1F5FE, 0x1F391, 0x1F3DE, 0x1F305, 0x1F304, 0x1F320,
          0x1F387, 0x1F386, 0x1F307, 0x1F306, 0x1F3D9, 0x1F303, 0x1F30C, 0x1F309,
        ],
        // eslint-disable-next-line max-len
        icon: '<svg id="transport" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 150 150"><path id="transport" d="M120.7,116H31a4.55,4.55,0,0,1-4.54-4.55V54.28A31.82,31.82,0,0,1,58.25,22.49h35.2a31.83,31.83,0,0,1,31.8,31.79v57.15A4.55,4.55,0,0,1,120.7,116Zm-85.16-9.09h80.62V54.28A22.74,22.74,0,0,0,93.45,31.57H58.25A22.74,22.74,0,0,0,35.54,54.28v52.61Z"/><path id="transport" d="M49.35,129.23c-8.53,0-13.62-2.77-13.62-7.41V115.6a4.54,4.54,0,1,1,9.08,0v4.06a21.32,21.32,0,0,0,9.09,0V115.6a4.54,4.54,0,0,1,9.08,0v6.22c0,4.64-5.09,7.41-13.63,7.41Z"/><path id="transport" d="M102.34,129.23c-8.53,0-13.62-2.77-13.62-7.41V115.6a4.54,4.54,0,0,1,9.08,0v4.06a21.28,21.28,0,0,0,9.08,0V115.6a4.55,4.55,0,0,1,9.09,0v6.22c0,4.64-5.09,7.41-13.63,7.41Z"/><path id="transport" d="M97.81,44.83H53.9a4.55,4.55,0,1,1,0-9.09H97.81a4.55,4.55,0,0,1,0,9.09Z"/><path id="transport" d="M54.28,84.2A6.8,6.8,0,1,0,61.07,91a6.8,6.8,0,0,0-6.79-6.8Z"/><path id="transport" d="M97.43,84.2a6.8,6.8,0,1,0,6.79,6.8,6.8,6.8,0,0,0-6.79-6.8Z"/><path id="transport" d="M107.08,81H44.63a6.82,6.82,0,0,1-6.82-6.82V54.28a6.82,6.82,0,0,1,6.82-6.81h62.45a6.82,6.82,0,0,1,6.81,6.81V74.15A6.83,6.83,0,0,1,107.08,81ZM44.63,52a2.28,2.28,0,0,0-2.28,2.27V74.15a2.28,2.28,0,0,0,2.28,2.27h62.45a2.27,2.27,0,0,0,2.27-2.27V54.28A2.27,2.27,0,0,0,107.08,52Z"/></svg>',
      },
      {
        name: 'objects',
        items: [
          0x1F4F1, 0x1F4F2, 0x1F4BB, 0x1F5A5, 0x1F5A8, 0x1F5B1, 0x1F5B2, 0x1F579,
          0x1F5DC, 0x1F4BD, 0x1F4BE, 0x1F4BF, 0x1F4C0, 0x1F4FC, 0x1F4F7, 0x1F4F8,
          0x1F4F9, 0x1F3A5, 0x1F4FD, 0x1F39E, 0x1F4DE, 0x1F4DF, 0x1F4E0, 0x1F4FA,
          0x1F4FB, 0x1F399, 0x1F39A, 0x1F39B, 0x23F1, 0x23F2, 0x23F0, 0x1F570,
          0x23F3, 0x1F4E1, 0x1F50B, 0x1F50C, 0x1F4A1, 0x1F526, 0x1F56F, 0x1F5D1,
          0x1F6E2, 0x1F4B8, 0x1F4B5, 0x1F4B4, 0x1F4B6, 0x1F4B7, 0x1F4B0, 0x1F4B3,
          0x1F9FE, 0x1F48E, 0x1F527, 0x1F528, 0x2692, 0x1F6E0, 0x26CF, 0x1F529,
          0x26D3, 0x1F52B, 0x1F4A3, 0x1F52A, 0x1F5E1, 0x1F6E1, 0x1F6AC, 0x1F3FA,
          0x1F9ED, 0x1F9F1, 0x1F52E, 0x1F9FF, 0x1F9F8, 0x1F4FF, 0x1F488, 0x1F52D,
          0x1F9F0, 0x1F9F2, 0x1F9EA, 0x1F9EB, 0x1F9EC, 0x1F9EF, 0x1F52C, 0x1F573,
          0x1F48A, 0x1F489, 0x1F321, 0x1F6BD, 0x1F6B0, 0x1F6BF, 0x1F6C1, 0x1F6C0,
          0x1F9F4, 0x1F9F5, 0x1F9F6, 0x1F9F7, 0x1F9F9, 0x1F9FA, 0x1F9FB, 0x1F9FC,
          0x1F9FD, 0x1F6CE, 0x1F511, 0x1F5DD, 0x1F6AA, 0x1F6CB, 0x1F6CF, 0x1F6CC,
          0x1F5BC, 0x1F6CD, 0x1F9F3, 0x1F6D2, 0x1F381, 0x1F388, 0x1F38F, 0x1F380,
          0x1F38A, 0x1F389, 0x1F9E8, 0x1F38E, 0x1F3EE, 0x1F390, 0x1F9E7, 0x1F4E9,
          0x1F4E8, 0x1F4E7, 0x1F48C, 0x1F4E5, 0x1F4E4, 0x1F4E6, 0x1F3F7, 0x1F4EA,
          0x1F4EB, 0x1F4EC, 0x1F4ED, 0x1F4EE, 0x1F4EF, 0x1F4DC, 0x1F4C3, 0x1F4C4,
          0x1F4D1, 0x1F4CA, 0x1F4C8, 0x1F4C9, 0x1F5D2, 0x1F5D3, 0x1F4C6, 0x1F4C5,
          0x1F4C7, 0x1F5C3, 0x1F5F3, 0x1F5C4, 0x1F4CB, 0x1F4C1, 0x1F4C2, 0x1F5C2,
          0x1F5DE, 0x1F4F0, 0x1F4D3, 0x1F4D4, 0x1F4D2, 0x1F4D5, 0x1F4D7, 0x1F4D8,
          0x1F4D9, 0x1F4DA, 0x1F4D6, 0x1F516, 0x1F517, 0x1F4CE, 0x1F587, 0x1F4D0,
          0x1F4CF, 0x1F4CC, 0x1F4CD, 0x1F58A, 0x1F58B, 0x1F58C, 0x1F58D, 0x1F4DD,
          0x1F50D, 0x1F50E, 0x1F50F, 0x1F510, 0x1F512, 0x1F513, 0x1F9E1, 0x1F49B,
          0x1F49A, 0x1F499, 0x1F49C, 0x1F5A4, 0x1F494, 0x1F495, 0x1F49E, 0x1F493,
          0x1F497, 0x1F496, 0x1F498, 0x1F49D, 0x1F49F, 0x1F549, 0x1F52F, 0x1F54E,
          0x1F6D0, 0x26CE, 0x1F194, 0x1F251, 0x1F4F4, 0x1F4F3, 0x1F236, 0x1F238,
          0x1F23A, 0x1F19A, 0x1F4AE, 0x1F250, 0x1F234, 0x1F235, 0x1F239, 0x1F232,
          0x1F18E, 0x1F191, 0x1F198, 0x274C, 0x1F6D1, 0x1F4DB, 0x1F6AB, 0x1F4AF,
          0x1F4A2, 0x1F6B7, 0x1F6AF, 0x1F6B3, 0x1F6B1, 0x1F51E, 0x1F4F5, 0x1F6AD,
          0x2755, 0x2753, 0x2754, 0x1F505, 0x1F506, 0x1F6B8, 0x1F531, 0x1F530,
          0x2705, 0x1F4B9, 0x274E, 0x1F310, 0x1F4A0, 0x1F300, 0x1F4A4, 0x1F3E7,
          0x1F6BE, 0x1F233, 0x1F6C2, 0x1F6C3, 0x1F6C4, 0x1F6C5, 0x1F6B9, 0x1F6BA,
          0x1F6BC, 0x1F6BB, 0x1F6AE, 0x1F3A6, 0x1F4F6, 0x1F201, 0x1F523, 0x1F524,
          0x1F521, 0x1F520, 0x1F196, 0x1F197, 0x1F199, 0x1F192, 0x1F195, 0x1F193,
          0x1F51F, 0x1F522, 0x23F8, 0x23EF, 0x23F9, 0x23FA, 0x23ED, 0x23EE, 0x23E9,
          0x23EA, 0x23EB, 0x23EC, 0x1F53C, 0x1F53D, 0x1F500, 0x1F501, 0x1F502,
          0x1F504, 0x1F503, 0x1F3B5, 0x1F3B6, 0x2795, 0x2796, 0x2797, 0x267E,
          0x1F4B2, 0x1F4B1, 0x27B0, 0x27BF, 0x1F51A, 0x1F519, 0x1F51B, 0x1F51D,
          0x1F51C, 0x1F518, 0x1F534, 0x1F535, 0x1F53A, 0x1F53B, 0x1F538, 0x1F539,
          0x1F536, 0x1F537, 0x1F533, 0x1F532, 0x1F508, 0x1F507, 0x1F509, 0x1F50A,
          0x1F514, 0x1F515, 0x1F4E3, 0x1F4E2, 0x1F4AC, 0x1F4AD, 0x1F5EF, 0x1F0CF,
          0x1F3B4, 0x1F550, 0x1F551, 0x1F552, 0x1F553, 0x1F554, 0x1F555, 0x1F556,
          0x1F557, 0x1F558, 0x1F559, 0x1F55A, 0x1F55B, 0x1F55C, 0x1F55D, 0x1F55E,
          0x1F55F, 0x1F560, 0x1F561, 0x1F562, 0x1F563, 0x1F564, 0x1F565, 0x1F566,
          0x1F567,
        ],
        // eslint-disable-next-line max-len
        icon: '<svg id="objects" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 150 150"><path id="objects" d="M107.78,129a4.55,4.55,0,0,1-2.67-.87l-30-21.79-30,21.79a4.53,4.53,0,0,1-5.34,0,4.58,4.58,0,0,1-1.65-5.08L49.59,87.82,19.6,66a4.54,4.54,0,0,1,2.67-8.22H59.34L70.8,22.55a4.55,4.55,0,0,1,8.64,0L90.89,57.81H128A4.54,4.54,0,0,1,130.63,66l-30,21.79,11.46,35.25a4.55,4.55,0,0,1-4.32,6ZM75.12,96.2a4.53,4.53,0,0,1,2.67.87l21.35,15.51L91,87.49a4.55,4.55,0,0,1,1.65-5.08L114,66.89H87.59a4.54,4.54,0,0,1-4.32-3.13l-8.15-25.1L67,63.76a4.53,4.53,0,0,1-4.32,3.13H36.25L57.61,82.41a4.54,4.54,0,0,1,1.65,5.08l-8.17,25.09L72.45,97.07a4.53,4.53,0,0,1,2.67-.87Z"/></svg>',
      },
    ];

    this.init();
  }

  init() {
    const emojiInputs = document.querySelectorAll('[data-emoji="true"]');

    emojiInputs.forEach((element) => {
      this.generateElements(element);
    });
  }

  generateElements(emojiInput) {
    if (emojiInput.parentNode.classList.contains('emoji-picker-container')) {
      return;
    }

    const emojiContainer = document.createElement('div');
    emojiContainer.classList.add('emoji-picker-container');

    const parent = emojiInput.parentNode;
    parent.replaceChild(emojiContainer, emojiInput);
    emojiContainer.appendChild(emojiInput);

    const emojiPicker = document.createElement('div');
    emojiPicker.tabIndex = 0;
    emojiPicker.classList.add('emoji-picker');

    const emojiTrigger = document.createElement('span');
    emojiTrigger.classList.add('emoji-picker-trigger');

    // eslint-disable-next-line max-len
    emojiTrigger.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 12 14"><path d="M8.9 8.4q-0.3 0.9-1.1 1.5t-1.8 0.6-1.8-0.6-1.1-1.5q-0.1-0.2 0-0.4t0.3-0.2q0.2-0.1 0.4 0t0.2 0.3q0.2 0.6 0.7 1t1.2 0.4 1.2-0.4 0.7-1q0.1-0.2 0.3-0.3t0.4 0 0.3 0.2 0 0.4zM5 5q0 0.4-0.3 0.7t-0.7 0.3-0.7-0.3-0.3-0.7 0.3-0.7 0.7-0.3 0.7 0.3 0.3 0.7zM9 5q0 0.4-0.3 0.7t-0.7 0.3-0.7-0.3-0.3-0.7 0.3-0.7 0.7-0.3 0.7 0.3 0.3 0.7zM11 7q0-1-0.4-1.9t-1.1-1.6-1.6-1.1-1.9-0.4-1.9 0.4-1.6 1.1-1.1 1.6-0.4 1.9 0.4 1.9 1.1 1.6 1.6 1.1 1.9 0.4 1.9-0.4 1.6-1.1 1.1-1.6 0.4-1.9zM12 7q0 1.6-0.8 3t-2.2 2.2-3 0.8-3-0.8-2.2-2.2-0.8-3 0.8-3 2.2-2.2 3-0.8 3 0.8 2.2 2.2 0.8 3z"/></svg>';
    emojiTrigger.onclick = () => {
      emojiContainer.classList.toggle('emoji-picker-open');
      emojiPicker.focus();
    };
    emojiContainer.appendChild(emojiTrigger);

    window.addEventListener('click', (e) => {
      if (emojiContainer.classList.contains('emoji-picker-open')) {
        if (emojiPicker.contains(e.target) || emojiTrigger.contains(e.target)) {
          // emoji inserted
        } else {
          emojiContainer.classList.remove('emoji-picker-open');
        }
      }
    });

    const emojiCategory = document.createElement('ul');
    emojiCategory.classList.add('emoji-picker-tabs');
    emojiPicker.appendChild(emojiCategory);

    const clickEmoji = (event) => {
      // not really efficient way, using emojiInput will be faster, but cause issues when element was updated by primefaces
      const inputField = document.querySelector('.emoji-picker-open [data-emoji="true"]');
      const caretPos = inputField.selectionStart;
      inputField.value = `${inputField.value.substring(0, caretPos)} ${event.target.innerHTML}${inputField.value.substring(caretPos)}`;
      emojiContainer.classList.add('emoji-picker-open');
    };

    const clickCategory = (event) => {
      emojiContainer.classList.add('emoji-picker-open');

      const hideUls = emojiPicker.querySelectorAll('ul');
      for (let i = 1, l = hideUls.length; i < l; i++) {
        hideUls[i].style.display = 'none';
      }

      const backgroundToggle = emojiPicker.querySelectorAll('.emoji-picker-tabs .active');
      for (let i = 0, l = backgroundToggle.length; i < l; i++) {
        backgroundToggle[i].classList.remove('active');
      }

      emojiPicker.querySelector(`.emoji-picker-list-${event.target.id}`).style.display = 'block';
      emojiPicker.querySelector(`#${event.target.id}`).classList.add('active');
    };

    const renderEmoji = (item, category) => {
      const emojiLi = document.createElement('li');
      const emojiLink = document.createElement('span');
      emojiLink.classList.add('emoji-picker-emoji');
      emojiLink.innerHTML = String.fromCodePoint(item);
      emojiLink.onmousedown = clickEmoji;

      emojiLi.appendChild(emojiLink);
      category.appendChild(emojiLi);
    };

    this.emojiData.forEach((category, i) => {
      const emojiLi = document.createElement('li');
      const emojiEl = document.createElement('span');
      emojiEl.id = category.name;
      emojiEl.classList.add('emoji-picker-anchor');
      if (category.name === 'faces') {
        emojiEl.classList.add('active');
      }
      emojiEl.innerHTML = category.icon;
      emojiEl.onmousedown = clickCategory;

      emojiLi.appendChild(emojiEl);
      emojiCategory.appendChild(emojiLi);

      const categoryUl = document.createElement('ul');
      categoryUl.classList.add('emoji-picker-list', `emoji-picker-list-${category.name}`);
      categoryUl.style.display = i === 0 ? 'block' : 'none';

      category.items.forEach((item) => renderEmoji(item, categoryUl));

      emojiPicker.appendChild(categoryUl);
    });

    emojiContainer.appendChild(emojiPicker);
  }
}

const emoji = new Emoji();
